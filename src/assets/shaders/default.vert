attribute vec2 a_pos;
attribute vec4 a_color;
attribute vec2 a_uv;

uniform mat4 u_proj;

varying vec4 v_color;
varying vec2 v_uv;

void main() {
    v_color = a_color;
    v_uv = a_uv;
    gl_Position = u_proj * vec4(a_pos, 0, 1);
}
