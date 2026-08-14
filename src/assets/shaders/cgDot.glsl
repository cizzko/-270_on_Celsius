#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 1) readonly buffer Mask { int m[]; };
layout(std430, binding = 5) readonly buffer R { float r[]; };
layout(std430, binding = 6) readonly buffer Z { float z[]; };
layout(std430, binding = 7) readonly buffer Ap { float ap[]; };
layout(std430, binding = 8) buffer Partial { float partial[]; };
uniform int u_worldWidth, u_worldHeight;

shared float s_rr[256];
shared float s_rz[256];
shared float s_zAp[256];

void main() {
    uint gx = gl_GlobalInvocationID.x, gy = gl_GlobalInvocationID.y;
    uint lx = gl_LocalInvocationID.x, ly = gl_LocalInvocationID.y;
    uint tid = ly * 16u + lx;
    uint groupIdx = gy * gl_NumWorkGroups.x + gx;

    float rr = 0.0, rz = 0.0, zAp = 0.0;
    if (gx < u_worldWidth && gy < u_worldHeight) {
        int idx = int(gy * u_worldWidth + gx);
        if (m[idx] == 0) {
            rr = r[idx] * r[idx];
            rz = r[idx] * z[idx];
            zAp = z[idx] * ap[idx];
        }
    }

    s_rr[tid] = rr;
    s_rz[tid] = rz;
    s_zAp[tid] = zAp;
    groupMemoryBarrier();
    barrier();

    for (uint stride = 128u; stride > 0u; stride >>= 1) {
        if (tid < stride) {
            s_rr[tid] += s_rr[tid + stride];
            s_rz[tid] += s_rz[tid + stride];
            s_zAp[tid] += s_zAp[tid + stride];
        }
        groupMemoryBarrier();
        barrier();
    }

    if (tid == 0u) {
        partial[groupIdx * 3u + 0u] = s_rr[0];
        partial[groupIdx * 3u + 1u] = s_rz[0];
        partial[groupIdx * 3u + 2u] = s_zAp[0];
    }
}