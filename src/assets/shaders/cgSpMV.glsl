#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 1) readonly buffer Mask { int m[]; };
layout(std430, binding = 5) readonly buffer Z { float z[]; };
layout(std430, binding = 6) buffer Ap { float ap[]; };
uniform int u_worldWidth, u_worldHeight;

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    if (m[idx] != 0) {
        ap[idx] = 0.0;
        return;
    }

    int leftX = (x == 0) ? u_worldWidth - 1 : int(x - 1u);
    int rightX = (x == u_worldWidth - 1) ? 0 : int(x + 1u);
    int idxL = int(y) * u_worldWidth + leftX;
    int idxR = int(y) * u_worldWidth + rightX;

    float zL = (m[idxL] != 0) ? z[idx] : z[idxL];
    float zR = (m[idxR] != 0) ? z[idx] : z[idxR];

    float zD, zU;
    if (y == 0u) zD = z[idx];
    else {
        int idxD = int(y - 1u) * u_worldWidth + int(x);
        zD = (m[idxD] != 0) ? z[idx] : z[idxD];
    }
    if (y == uint(u_worldHeight - 1)) zU = 0.0;
    else {
        int idxU = int(y + 1u) * u_worldWidth + int(x);
        zU = (m[idxU] != 0) ? z[idx] : z[idxU];
    }

    ap[idx] = 4.0 * z[idx] - (zL + zR + zD + zU);
}