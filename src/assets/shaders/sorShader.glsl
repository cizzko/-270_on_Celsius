#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 1) readonly buffer Mask { int m[]; };
layout(std430, binding = 5) buffer Density { float p[]; };
layout(std430, binding = 6) readonly buffer Div { float div[]; };
uniform float u_omega;
uniform int u_offset;
uniform int u_worldWidth, u_worldHeight;
shared float pLocal[18][19];
shared int maskLocal[18][19];

void main() {
    uint gx = gl_GlobalInvocationID.x, gy = gl_GlobalInvocationID.y;
    uint lx = gl_LocalInvocationID.x, ly = gl_LocalInvocationID.y;
    if (gx < u_worldWidth && gy < u_worldHeight) {
        int idx = int(gy * u_worldWidth + gx);
        pLocal[lx+1][ly+1] = p[idx]; maskLocal[lx+1][ly+1] = m[idx];
    } else { pLocal[lx+1][ly+1] = 0.0; maskLocal[lx+1][ly+1] = 1; }
    if (lx == 0) {
        uint nx = (gx == 0) ? u_worldWidth - 1 : gx - 1; uint ny = gy;
        if (ny < u_worldHeight) {
            int idx = int(ny * u_worldWidth + nx); pLocal[0][ly+1] = p[idx]; maskLocal[0][ly+1] = m[idx];
        } else { pLocal[0][ly+1] = 0.0; maskLocal[0][ly+1] = 1; }
    }
    if (lx == 15) {
        uint nx = (gx == u_worldWidth - 1) ? 0 : gx + 1; uint ny = gy;
        if (ny < u_worldHeight) {
            int idx = int(ny * u_worldWidth + nx); pLocal[17][ly+1] = p[idx]; maskLocal[17][ly+1] = m[idx];
        } else { pLocal[17][ly+1] = 0.0; maskLocal[17][ly+1] = 1; }
    }
    if (ly == 0) {
        uint nx = gx;
        if (nx < u_worldWidth && gy > 0) {
            uint ny = gy - 1; int idx = int(ny * u_worldWidth + nx); pLocal[lx+1][0] = p[idx]; maskLocal[lx+1][0] = m[idx];
        } else { pLocal[lx+1][0] = 0.0; maskLocal[lx+1][0] = 1; }
    }
    if (ly == 15) {
        uint nx = gx; uint ny = gy + 1;
        if (nx < u_worldWidth && ny < u_worldHeight) {
            int idx = int(ny * u_worldWidth + nx); pLocal[lx+1][17] = p[idx]; maskLocal[lx+1][17] = m[idx];
        } else { pLocal[lx+1][17] = 0.0; maskLocal[lx+1][17] = 1; }
    }
    groupMemoryBarrier(); barrier();
    if (gx >= u_worldWidth || gy >= u_worldHeight) return;
    int idx = int(gy * u_worldWidth + gx);
    if (gy == uint(u_worldHeight - 1)) { p[idx] = 0.0; return; }
    if (maskLocal[lx+1][ly+1] != 0 || (gx + gy) % 2 != u_offset) return;
    float pL = (maskLocal[lx][ly+1] != 0) ? p[idx] : pLocal[lx][ly+1];
    float pR = (maskLocal[lx+2][ly+1] != 0) ? p[idx] : pLocal[lx+2][ly+1];
    float pD = (maskLocal[lx+1][ly] != 0) ? p[idx] : pLocal[lx+1][ly];
    float pU = (maskLocal[lx+1][ly+2] != 0) ? p[idx] : pLocal[lx+1][ly+2];
    float old = p[idx];
    p[idx] = (1.0 - u_omega) * old + u_omega * (div[idx] + pL + pR + pD + pU) * 0.25;
}