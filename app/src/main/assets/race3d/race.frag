precision mediump float;
uniform sampler2D uTexture;
uniform vec3 uCamera;
uniform vec3 uTint;
uniform vec2 uFog;
uniform float uKind;
varying mediump vec2 vUv;
varying mediump vec3 vNormal;
varying mediump vec3 vColor;
varying mediump vec3 vWorld;

void main() {
    vec3 normal = vNormal / max(length(vNormal), 0.001);
    vec3 sunlight = normalize(vec3(0.40, 0.88, -0.26));
    vec3 base = uTint * vColor;
    if ((uKind > 0.5 && uKind < 1.5) || uKind > 2.5) {
        base = uTint * texture2D(uTexture, vUv).rgb * mix(vec3(0.86), vColor, 0.62);
    } else if (uKind > 1.5 && uKind < 2.5) {
        // Quiet grass patch variation behind the surveyed-size road model.
        float patches = sin(vWorld.x * 0.095 + sin(vWorld.z * 0.083)) * sin(vWorld.z * 0.12);
        vec2 local = mod(vWorld.xz, 128.0);
        float grain = sin(local.x * 2.72 + local.y * 4.27) * sin(local.y * 3.73);
        base = mix(vec3(0.19, 0.255, 0.15), vec3(0.31, 0.35, 0.20), patches * 0.36 + 0.5);
        base += grain * 0.012;
    }
    vec3 shade = base * (0.66 + 0.42 * max(dot(normal, sunlight), 0.0));
    if (uKind > 2.5) {
        vec3 halfVector = normalize(sunlight + normalize(uCamera - vWorld));
        shade += pow(max(dot(normal, halfVector), 0.0), 24.0) * 0.30;
    }
    float fog = smoothstep(uFog.x, uFog.y, max(abs(vWorld.x - uCamera.x), abs(vWorld.z - uCamera.z)));
    gl_FragColor = vec4(mix(shade, vec3(0.63, 0.74, 0.82), fog * 0.87), 1.0);
}
