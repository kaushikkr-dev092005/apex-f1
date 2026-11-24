uniform mat4 uViewProjection;
uniform mat4 uModel;
uniform vec2 uUvScale;
attribute vec3 aPosition;
attribute vec2 aUv;
attribute vec3 aNormal;
attribute vec3 aColor;
varying mediump vec2 vUv;
varying mediump vec3 vNormal;
varying mediump vec3 vColor;
varying mediump vec3 vWorld;

void main() {
    vec4 world = uModel * vec4(aPosition, 1.0);
    vWorld = world.xyz;
    vUv = aUv * uUvScale;
    vNormal = mat3(uModel) * aNormal;
    vColor = aColor;
    gl_Position = uViewProjection * world;
}
