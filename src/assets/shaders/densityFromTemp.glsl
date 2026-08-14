#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 0) readonly buffer TIn { float tIn[]; };
layout(std430, binding = 1) buffer DensityOut { float dOut[]; };
uniform float u_thermalExpansion;
uniform float u_refTemp;
uniform int u_worldWidth, u_worldHeight;

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    float T = tIn[idx];
    float T_abs = T + 273.15;
    float T_ref_abs = u_refTemp + 273.15;
    float rho = T_ref_abs / max(T_abs, 1.0);
    dOut[idx] = clamp(rho, 0.1, 10.0);
}