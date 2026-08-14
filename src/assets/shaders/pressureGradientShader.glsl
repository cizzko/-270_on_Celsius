#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 1) readonly buffer Mask { int m[]; };
layout(std430, binding = 3) readonly buffer VxIn { float vxIn[]; };
layout(std430, binding = 4) readonly buffer VyIn { float vyIn[]; };
layout(std430, binding = 5) readonly buffer P { float p[]; };
layout(std430, binding = 15) readonly buffer StaticP { float sp[]; };
layout(std430, binding = 8) buffer VxOut { float vxOut[]; };
layout(std430, binding = 9) buffer VyOut { float vyOut[]; };
uniform float u_dt;
uniform int u_worldWidth, u_worldHeight;

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    float isSolid = float(m[idx]);
    float cellActive = 1.0 - isSolid;

    int leftX  = (x == 0u ? u_worldWidth - 1 : int(x - 1u));
    int rightX = (x == u_worldWidth - 1u ? 0 : int(x + 1u));
    int idxL = int(y) * u_worldWidth + leftX;
    int idxR = int(y) * u_worldWidth + rightX;

    float totalP    = p[idx] + sp[idx];
    float totalPL   = p[idxL] + sp[idxL];
    float totalPR   = p[idxR] + sp[idxR];

    float pL = (m[idxL] != 0) ? totalP : totalPL;
    float pR = (m[idxR] != 0) ? totalP : totalPR;

    float pD, pU;
    if (y == 0u) pD = totalP;
    else {
        int idxD = int(y-1u)*u_worldWidth + int(x);
        float totalPD = p[idxD] + sp[idxD];
        pD = (m[idxD] != 0) ? totalP : totalPD;
    }
    if (y == uint(u_worldHeight - 1)) pU = 0.0;
    else {
        int idxU = int(y+1u)*u_worldWidth + int(x);
        float totalPU = p[idxU] + sp[idxU];
        pU = (m[idxU] != 0) ? totalP : totalPU;
    }

    float nvx = vxIn[idx] - 0.5 * (pR - pL) * u_dt;
    float nvy = vyIn[idx] - 0.5 * (pU - pD) * u_dt;
    nvx = mix(0.0, nvx, cellActive * (1.0 - float(isnan(nvx) || isinf(nvx))));
    nvy = mix(0.0, nvy, cellActive * (1.0 - float(isnan(nvy) || isinf(nvy))));
    vxOut[idx] = nvx; vyOut[idx] = nvy;
}