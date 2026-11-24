"""Pack the supplied OBJ grid and textured GLB circuits for the Android renderer.

Requires NumPy on the workstation doing the import. The APK builds from the
generated assets and has no Python or glTF loading dependency.
"""
import argparse
import hashlib
from io import BytesIO
import json
import struct
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

GRID_SIZE = 22


# Interleaved GPU layout. Quantize UVs and normals while retaining metre
# positions so narrow markings don't drift and crossovers retain their height.
VERTEX = np.dtype([
    ("position", "<f4", 3),
    ("uv", "<i2", 2),
    ("normal", "i1", 4),
    ("color", "u1", 4),
])


def mesh_batch(position, normal, uv, color, indices, material):
    if not 0 < len(position) < 65536 or int(indices.max()) >= len(position):
        raise ValueError("Mesh exceeds GLES 2 index range")
    if not all(np.isfinite(a).all() for a in (position, normal, uv, color)):
        raise ValueError("Nonfinite mesh attribute")
    vertices = np.zeros(len(position), dtype=VERTEX)
    vertices["position"] = position
    uv_scale = np.maximum(np.abs(uv).max(axis=0), 1)
    vertices["uv"] = np.rint(uv / uv_scale * 32767)
    vertices["normal"][:, :3] = np.rint(normal.clip(-1, 1) * 127)
    vertices["color"][:, :3] = color[:, :3]
    vertices["color"][:, 3] = 255
    return struct.pack("<iiiff", material, len(vertices), indices.size, *uv_scale) + (
        vertices.tobytes() + indices.astype("<u2").tobytes()
    )


def write_mesh(path, batches):
    path.write_bytes(struct.pack("<4si", b"APXM", len(batches)) + b"".join(batches))


def glb_reader(raw):
    magic, version, size, header_size, chunk_type = struct.unpack_from("<4sIII4s", raw)
    if (magic, version, size, chunk_type) != (b"glTF", 2, len(raw), b"JSON"):
        raise ValueError("Expected a GLB 2.0 circuit")
    document = json.loads(raw[20:20 + header_size])
    binary = memoryview(raw)[28 + header_size:]

    def read_view(index):
        view = document["bufferViews"][index]
        offset = view.get("byteOffset", 0)
        return binary[offset:offset + view["byteLength"]]

    def attribute(index):
        accessor = document["accessors"][index]
        types = {5121: "u1", 5123: "<u2", 5126: "<f4"}
        widths = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}
        return np.frombuffer(
            read_view(accessor["bufferView"]), dtype=types[accessor["componentType"]],
            count=accessor["count"] * widths[accessor["type"]],
            offset=accessor.get("byteOffset", 0),
        ).reshape(accessor["count"], -1)

    return document, read_view, attribute


def pack_glb(raw, destination, textures):
    document, read_view, attribute = glb_reader(raw)
    for image in document["images"]:
        name = image["name"] + ".jpg"
        payload = bytes(read_view(image["bufferView"]))
        # The source repeats identical tiles in every circuit. Keep one copy.
        if name in textures and textures[name] != payload:
            raise ValueError(f"Different images share the name {name}")
        textures[name] = payload

    batches = []
    for node in document["nodes"]:
        if "mesh" not in node:
            continue
        if any(key in node for key in ("matrix", "translation", "rotation", "scale")):
            raise ValueError("Circuit surface transform requires baking")
        for primitive in document["meshes"][node["mesh"]]["primitives"]:
            if primitive.get("mode", 4) != 4:
                raise ValueError("Expected triangle primitives")
            attributes = primitive["attributes"]
            position = attribute(attributes["POSITION"])
            color = attribute(attributes["COLOR_0"]) if "COLOR_0" in attributes else np.full((len(position), 3), 255)
            batches.append(mesh_batch(
                position, attribute(attributes["NORMAL"]), attribute(attributes["TEXCOORD_0"]),
                color, attribute(primitive["indices"]), primitive["material"],
            ))
    write_mesh(destination, batches)


def obj_bounds(raw):
    positions = []
    parts = set()
    for line in raw.decode("utf-8").splitlines():
        fields = line.split()
        if not fields:
            continue
        if fields[0] == "v":
            x, longitudinal, height = map(float, fields[1:4])
            positions.append((x, height, -longitudinal))
        elif fields[0] == "o":
            parts.add(fields[1])
    return np.ptp(positions, axis=0), parts


def encode_image(data, output, max_side):
    with Image.open(BytesIO(data)) as image:
        color = image.convert("RGB")
        color.thumbnail((max_side, max_side), Image.Resampling.LANCZOS)
        if color.width & (color.width - 1) or color.height & (color.height - 1):
            raise ValueError("Race textures must have power-of-two dimensions")
        color.save(output, "WEBP", quality=89, method=6)


def pack_livery_grid(downloads, root, bounds, obj_parts):
    archive_path = downloads / "F1-Car-Liveries(1).zip"
    cars = []
    with zipfile.ZipFile(archive_path) as archive:
        entries = sorted(file for file in archive.namelist() if file.endswith(".glb"))
        if len(entries) != GRID_SIZE:
            raise ValueError(f"Expected {GRID_SIZE} livery models")
        geometry_hash = None
        for slot, file in enumerate(entries):
            glb, read_view, attribute = glb_reader(archive.read(file))
            fingerprint = hashlib.sha256()
            for index, view in enumerate(glb["bufferViews"]):
                if view.get("target"):
                    fingerprint.update(read_view(index))
            extras = glb["scenes"][glb["scene"]]["extras"]
            image_index = glb["textures"][1]["source"]
            filename = f"livery-{slot + 1}.webp"
            encode_image(read_view(glb["images"][image_index]["bufferView"]), root / filename, 1024)
            team = extras["team"]
            number = extras["race_number"]
            cars.append({"team": team, "number": number, "texture": filename})
            if slot == 0:
                geometry_hash = fingerprint.digest()
                parts = {node["name"] for node in glb["nodes"]}
                if parts != obj_parts:
                    raise ValueError("Liveried GLBs do not match the supplied OBJ parts")
                pack_liveried_mesh(glb, attribute, root, bounds)
                for image_id, name in ((0, "carbon"), (2, "tyre")):
                    source = glb["textures"][image_id]["source"]
                    encode_image(read_view(glb["images"][source]["bufferView"]), root / (name + ".webp"), 256)
            elif fingerprint.digest() != geometry_hash:
                raise ValueError(f"Unexpected geometry variation for {file}")
    if len({car["number"] for car in cars}) != GRID_SIZE:
        raise ValueError("Race numbers must identify distinct liveries")
    (root / "grid.json").write_text(json.dumps(cars), encoding="utf-8")


def pack_liveried_mesh(document, attribute, root, obj_dimensions):
    groups = {material: [] for material in (5, 6, 7, 9)}
    for node in document["nodes"]:
        if any(key in node for key in ("matrix", "scale", "children")):
            raise ValueError("Unexpected car mesh transform")
        rotation = node.get("rotation", [0, 0, 0, 1])
        translation = node.get("translation", [0, 0, 0])

        def rotate(vectors):
            tangent = 2 * np.cross(rotation[:3], vectors)
            return vectors + rotation[3] * tangent + np.cross(rotation[:3], tangent)

        for primitive in document["meshes"][node["mesh"]]["primitives"]:
            material = primitive["material"]
            vertices = primitive["attributes"]
            tint = document["materials"][material]["pbrMetallicRoughness"].get("baseColorFactor", [1, 1, 1, 1])
            category = {0: 5, 1: 6, 10: 6, 6: 7}.get(material, 9)
            position = rotate(attribute(vertices["POSITION"])) + translation
            srgb = (np.power(tint[:3], 1 / 2.2) * 255).round().astype(np.uint8)
            groups[category].append((
                position, rotate(attribute(vertices["NORMAL"])), attribute(vertices["TEXCOORD_0"]),
                np.tile(srgb, (len(position), 1)), attribute(primitive["indices"]).astype(np.int32).reshape(-1, 3),
            ))

    position = np.concatenate([part[0] for parts in groups.values() for part in parts])
    low, high = position.min(axis=0), position.max(axis=0)
    # The livery export uses the same mesh with UV material splits. Check
    # proportions independent of exporter unit scale before using its UVs.
    if not np.allclose((high - low) / (high[2] - low[2]), obj_dimensions / obj_dimensions[2], atol=.025):
        raise ValueError("Livery geometry proportions differ from the OBJ")
    origin = np.array([(low[0] + high[0]) / 2, low[1], (low[2] + high[2]) / 2])
    scale = 5.4 / (high[2] - low[2])
    for far in (False, True):
        batches = []
        total = 0
        for category, parts in groups.items():
            vertices, normals, uv, colors = [np.concatenate(values) for values in zip(*(part[:4] for part in parts))]
            vertices = (vertices - origin) * scale
            indices = np.concatenate([part[4] + offset for part, offset in zip(
                parts, np.cumsum([0] + [len(part[0]) for part in parts[:-1]])
            )])
            if far:
                # UV-aware clustering keeps the original paint island mapping.
                keys = np.column_stack((np.rint(vertices / .11), np.rint(uv * 28), colors))
                _, first, welded = np.unique(keys, axis=0, return_index=True, return_inverse=True)
                vertices, normals, uv, colors = (values[first] for values in (vertices, normals, uv, colors))
                indices = welded[indices]
                a, b, c = indices.T
                indices = indices[(a != b) & (b != c) & (a != c)]
            batches.append(mesh_batch(vertices, normals, uv, colors, indices, category))
            total += len(indices)
        write_mesh(root / ("car-far.mesh" if far else "car-1.mesh"), batches)
        print(f"{'Distance' if far else 'Close-up'} car: {total:,} triangles; 5.4 m long")


def pack_route(data, output):
    columns = ["s_m", "x_m", "y_m", "z_m", "road_width_m", "crossfall_dy_per_positive_lateral_m"]
    if data["route_columns"] != columns:
        raise ValueError("Unexpected route layout")
    points = np.asarray(data["route"], dtype="<f4")
    grid = np.asarray([
        [spawn["s_m"], *spawn["position_m"], *spawn["forward"]]
        for spawn in data["grid_spawns"][:GRID_SIZE]
    ], dtype="<f4")
    period = data["sample_chainage_period_m"]
    if (
        points.shape[1] != 6 or grid.shape != (GRID_SIZE, 7)
        or not np.isfinite(points).all() or not np.isfinite(grid).all()
        or not (np.diff(points[:, 0]) > 0).all()
        or not points[0, 0] == 0 < points[-1, 0] < period
        or data["min_road_width_m"] < 7
    ):
        raise ValueError(f"Invalid route or grid for {data['id']}")
    output.write_bytes(struct.pack("<4siff", b"APXR", len(points), period, data["min_road_width_m"])
                       + points.tobytes() + grid.tobytes())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--downloads", type=Path, default=Path.home() / "Downloads")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1] / "app/src/main/assets/race3d"
    root.mkdir(parents=True, exist_ok=True)
    car_paths = [args.downloads / "car.obj"] + [args.downloads / f"car({i}).obj" for i in range(1, 10)]
    unique_obj = {}
    for path in car_paths:
        raw = path.read_bytes()
        unique_obj[hashlib.sha256(raw).hexdigest()] = raw
    if len(unique_obj) != 1:
        raise ValueError("This livery pack is mapped to one shared OBJ geometry")
    bounds, parts = obj_bounds(next(iter(unique_obj.values())))
    pack_livery_grid(args.downloads, root, bounds, parts)
    textures = {}
    with zipfile.ZipFile(args.downloads / "F1-2027-Minimal-Tracks(1).zip") as archive:
        prefix = "F1-2027-Minimal-Tracks/"
        index = json.loads(archive.read(prefix + "track-index.json"))
        catalog = json.loads((root.parent / "circuits-2027.json").read_text(encoding="utf-8"))
        if len(index) != 24 or {x["id"] for x in index} != {x["id"] for x in catalog}:
            raise ValueError("Track pack and calendar must contain the same 24 venues")
        for entry in index:
            track_id, file = entry["id"], entry["file"]
            pack_glb(archive.read(prefix + "Tracks/" + file), root / (track_id + ".mesh"), textures)
            metadata = json.loads(archive.read(prefix + "track-data/" + Path(file).with_suffix(".json").name))
            pack_route(metadata, root / (track_id + ".route"))
        for file in ("F1-Circuits-MIT-License.md", "Mapzen-Terrain-Attribution.md"):
            (root / file).write_bytes(archive.read(prefix + "Attribution/" + file))
    for name, raw in textures.items():
        (root / name).write_bytes(raw)
    print(f"Packed 24 circuits, all {GRID_SIZE} liveries, one shared car model and {len(textures)} road textures")


if __name__ == "__main__":
    main()
