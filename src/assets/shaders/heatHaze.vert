layout(location = 0) in vec2 a_pos;
layout(location = 1) in vec2 a_uv;
layout(location = 2) in float a_intensity;

uniform vec2 u_camera_origin;
uniform vec2 u_camera_size;

out vec2 v_uv;
out vec2 v_world;
out float v_intensity;

void main() {
    gl_Position = vec4(a_pos, 0, 1);
    v_uv = a_uv;
    v_intensity = a_intensity;
    v_world = u_camera_origin + (u_camera_size * v_uv);
}