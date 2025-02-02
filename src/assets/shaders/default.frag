#version 460

layout(binding = 0) uniform sampler2D u_texture;

layout(location = 0) in vec4 v_color;
layout(location = 1) in vec2 v_uv;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 c = texture(u_texture, v_uv);
    fragColor = v_color * c;
}
