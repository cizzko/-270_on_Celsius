#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 1) readonly buffer Mask { int m[]; };
layout(std430, binding = 5) buffer Pressure { float pressure[]; };
layout(std430, binding = 6) buffer R { float r[]; };
layout(std430, binding = 7) buffer Z { float z[]; };
layout(std430, binding = 8) readonly buffer Ap { float ap[]; };
uniform float u_alpha;
uniform int u_worldWidth, u_worldHeight;

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    if (m[idx] != 0) return;

    pressure[idx] += u_alpha * z[idx];
    r[idx] -= u_alpha * ap[idx];
    z[idx] = r[idx] * 0.25;
}