uniform sampler2D u_texture;
uniform vec2 u_reg_uv;
uniform vec2 u_reg_size;

in flat vec4 v_color;
in vec2 v_uv;

out vec4 fragColor;

void main() {
    vec2 fuv = u_reg_uv + (u_reg_size * fract(v_uv));
    vec2 dx = dFdx(v_uv) * u_reg_size;
    vec2 dy = dFdy(v_uv) * u_reg_size;
    fragColor = v_color * textureGrad(u_texture, fuv, dx, dy);
}
