uniform vec2 u_logical_ratio;
uniform vec2 u_camera_pos;

in vec2 a_pos;
in vec4 a_color;
in vec2 a_uv;

out flat vec4 v_color;
out vec2 v_uv;

void main() {
    v_color = a_color;
    v_uv = a_uv;

    vec2 local_to_cam = a_pos - u_camera_pos;
    vec2 clip_space_pos = local_to_cam * u_logical_ratio;
    gl_Position = vec4(clip_space_pos, 0, 1);
}
