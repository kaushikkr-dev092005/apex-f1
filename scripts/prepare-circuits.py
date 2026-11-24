"""Reproduce the small bundled circuit collection; not needed to build or run Android.

Input: bacinger/f1-circuits f1-circuits.geojson + LICENSE.md (MIT).
Calendar: Formula 1's 16 September 2026 announcement.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parent.parent
SOURCE_SHA256 = "7dcd3b9a5fe25b46c7ea276ea513475854a4187cebcf4384f9cd9a3465deee55"
ROUNDS = [
    ("bh-2002", "Bahrain", "12–14 MAR"),
    ("sa-2021", "Saudi Arabia", "19–21 MAR"),
    ("au-1953", "Australia", "02–04 APR"),
    ("jp-1962", "Japan", "09–11 APR"),
    ("cn-2004", "China", "16–18 APR"),
    ("us-2022", "Miami", "30 APR–02 MAY"),
    ("ca-1978", "Canada", "21–23 MAY"),
    ("mc-1929", "Monaco", "04–06 JUN"),
    ("pt-2008", "Portugal", "18–20 JUN"),
    ("gb-1948", "Great Britain", "02–04 JUL"),
    ("at-1969", "Austria", "09–11 JUL"),
    ("be-1925", "Belgium", "23–25 JUL"),
    ("hu-1986", "Hungary", "30 JUL–01 AUG"),
    ("it-1922", "Italy", "03–05 SEP"),
    ("es-2026", "Spain", "10–12 SEP"),
    ("az-2016", "Azerbaijan", "24–26 SEP"),
    ("tr-2005", "Türkiye", "01–03 OCT"),
    ("sg-2008", "Singapore", "08–10 OCT"),
    ("us-2012", "United States", "22–24 OCT"),
    ("mx-1962", "Mexico", "29–31 OCT"),
    ("br-1940", "Brazil", "05–07 NOV"),
    ("us-2023", "Las Vegas", "18–20 NOV"),
    ("qa-2004", "Qatar", "03–05 DEC"),
    ("ae-2009", "Abu Dhabi", "10–12 DEC"),
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=ROOT / ".tools/reference-video/f1-circuits.geojson")
    parser.add_argument("--license", type=Path, default=ROOT / ".tools/reference-video/LICENSE.md")
    args = parser.parse_args()
    raw = args.source.read_bytes()
    if hashlib.sha256(raw).hexdigest() != SOURCE_SHA256:
        raise ValueError("Source differs from reviewed geometry snapshot. Review geometry and licensing before updating the hash.")
    features = {f["properties"]["id"]: f for f in json.loads(raw)["features"]}
    circuits = []
    for round_number, (key, country, weekend) in enumerate(ROUNDS, 1):
        feature = features[key]
        coords = feature["geometry"]["coordinates"]
        latitude = sum(p[1] for p in coords) / len(coords)
        xy = [(p[0] * math.cos(math.radians(latitude)), -p[1]) for p in coords]
        xs, ys = [p[0] for p in xy], [p[1] for p in xy]
        span = max(max(xs) - min(xs), max(ys) - min(ys))
        cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
        circuits.append(dict(
            id=key, round=round_number, country=country,
            circuit=feature["properties"]["Name"], location=feature["properties"]["Location"],
            weekend=weekend, sprint=round_number in {1, 3, 4, 7, 8, 10, 14, 21, 23, 24},
            homologationPending=key == "tr-2005",
            points=[[round((x - cx) / span, 5), round((y - cy) / span, 5)] for x, y in xy],
        ))
    corrections = {
        "mx-1962": {"circuit": "Autódromo Hermanos Rodríguez"},
        "br-1940": {"circuit": "Interlagos", "location": "São Paulo"},
        "pt-2008": {"circuit": "Algarve International Circuit", "location": "Portimão"},
    }
    for circuit in circuits:
        circuit.update(corrections.get(circuit["id"], {}))
    assets = ROOT / "app/src/main/assets"
    assets.mkdir(parents=True, exist_ok=True)
    (assets / "circuits-2027.json").write_text(json.dumps(circuits, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    (assets / "licenses").mkdir(exist_ok=True)
    shutil.copyfile(args.license, assets / "licenses/f1-circuits-MIT.txt")
    print(f"Prepared {len(circuits)} circuits; {sum(len(c['points']) for c in circuits)} vertices.")


if __name__ == "__main__":
    main()
