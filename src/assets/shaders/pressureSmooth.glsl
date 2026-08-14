#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 1) readonly buffer Mask { int m[]; };
layout(std430, binding = 5) buffer Pressure { float p[]; };
uniform int u_worldWidth, u_worldHeight;

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    if (m[idx] != 0) return;

    if (y == u_worldHeight - 1u) {
        p[idx] = 0.0;
        return;
    }

    int leftX  = (x == 0u ? u_worldWidth - 1 : int(x - 1u));
    int rightX = (x == u_worldWidth - 1u ? 0 : int(x + 1u));
    int idxL = int(y) * u_worldWidth + leftX;
    int idxR = int(y) * u_worldWidth + rightX;

    float pL = (m[idxL] != 0) ? p[idx] : p[idxL];
    float pR = (m[idxR] != 0) ? p[idx] : p[idxR];

    float pD = p[idx], pU = p[idx];
    if (y > 0u) {
        int idxD = int(y-1u)*u_worldWidth + int(x);
        pD = (m[idxD] != 0) ? p[idx] : p[idxD];
    }
    if (y < uint(u_worldHeight - 1)) {
        int idxU = int(y+1u)*u_worldWidth + int(x);
        pU = (m[idxU] != 0) ? p[idx] : p[idxU];
    }

    p[idx] = 0.5 * p[idx] + 0.125 * (pL + pR + pD + pU);
}