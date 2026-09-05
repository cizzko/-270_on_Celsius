uniform sampler2D u_scene;
uniform sampler2D u_tempRate;
uniform vec2  u_camera_size;
uniform vec2  u_mask_origin;
uniform vec2  u_mask_cells;
uniform vec2  u_region_cells;
uniform float u_time;
uniform float u_visCut;
uniform float u_zoom;
uniform float u_flat;
uniform float u_maskOverlay;

in vec2 v_uv;
in vec2 v_world;
in float v_intensity;

out vec4 fragColor;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amp * noise(p);
        p *= 2.0;
        amp *= 0.5;
    }
    return value;
}

vec2 flow(vec2 q) {
    const float e = 0.5;
    float dX = fbm(q + vec2(e, 0.0)) - fbm(q - vec2(e, 0.0));
    float dY = fbm(q + vec2(0.0, e)) - fbm(q - vec2(0.0, e));
    return vec2(-dY, dX);
}

void main() {
    if (u_flat > 0.5) {
        fragColor = texture(u_scene, v_uv);
        return;
    }

    float intensity = v_intensity;

    if (u_maskOverlay > 0.5) {
        float overlayIntensity = v_intensity;
        fragColor = vec4(vec3(overlayIntensity), overlayIntensity);
        return;
    }

    float freq = 4.32;
    float ampWorld = 0.07;

    intensity = smoothstep(u_visCut, u_visCut * 2.0, intensity);

    vec2 q = v_world * freq;
    vec2 t = vec2(u_time * 0.24, u_time * 0.18) * (freq / 0.09);
    float breathe = 0.8 + 0.2 * cos(u_time * 78.0);

    vec2 flowv = flow(q + t);
    float el = length(flowv);
    vec2 dir = el > 1e-4 ? flowv / el : vec2(1.0, 0.0);
    vec2 per = vec2(-dir.y, dir.x);
    float base = noise(q * 1.7 + vec2(13.7, 7.1)) - 0.5;

    vec2 offWorld = (flowv * 1.5 + per * (base * 0.3)) * ampWorld * breathe * intensity;
    vec2 offUv = (offWorld * u_zoom) / u_camera_size;
    vec4 color = texture(u_scene, v_uv + offUv);

    fragColor = color;
}