varying vec4 v_color;
varying vec2 v_uv;

uniform sampler2D u_texture;

void main() {
    vec4 c = texture2D(u_texture, v_uv);
    gl_FragColor = v_color * c;
}
