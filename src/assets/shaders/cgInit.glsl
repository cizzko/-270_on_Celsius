#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 1) readonly buffer Mask { int m[]; };
layout(std430, binding = 5) buffer Pressure { float pressure[]; };
layout(std430, binding = 6) readonly buffer Divergence { float divergence[]; };
layout(std430, binding = 7) buffer R { float r[]; };
layout(std430, binding = 8) buffer Z { float z[]; };
uniform int u_worldWidth, u_worldHeight;

float getA(int idx, float p, int x, int y) {
    int leftX = (x == 0) ? u_worldWidth - 1 : x - 1;
    int rightX = (x == u_worldWidth - 1) ? 0 : x + 1;
    int idxL = y * u_worldWidth + leftX;
    int idxR = y * u_worldWidth + rightX;

    float pL = (m[idxL] != 0) ? p : pressure[idxL];
    float pR = (m[idxR] != 0) ? p : pressure[idxR];

    float pD, pU;
    if (y == 0) pD = p;
    else {
        int idxD = (y - 1) * u_worldWidth + x;
        pD = (m[idxD] != 0) ? p : pressure[idxD];
    }
    if (y == u_worldHeight - 1) pU = 0.0;
    else {
        int idxU = (y + 1) * u_worldWidth + x;
        pU = (m[idxU] != 0) ? p : pressure[idxU];
    }

    return 4.0 * p - (pL + pR + pD + pU);
}

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    if (m[idx] != 0) {
        r[idx] = 0.0;
        z[idx] = 0.0;
        return;
    }

    float p = pressure[idx];
    float Ap = getA(idx, p, int(x), int(y));
    r[idx] = divergence[idx] - Ap;
    z[idx] = r[idx] * 0.25;
}