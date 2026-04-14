#version 330 core

uniform sampler2D u_texture;

in vec4 v_color;
in vec2 v_uv;

out vec4 fragColor;

void main() {
    vec4 c = texture(u_texture, v_uv);
    fragColor = v_color * c;
}
