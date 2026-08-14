#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 0) readonly buffer Solid { int s[]; };
layout(std430, binding = 1) readonly buffer Mask { int m[]; };
layout(std430, binding = 2) readonly buffer TIn { float tIn[]; };
layout(std430, binding = 3) readonly buffer VxIn { float vxIn[]; };
layout(std430, binding = 4) readonly buffer VyIn { float vyIn[]; };
layout(std430, binding = 7) buffer TOut { float tOut[]; };
layout(std430, binding = 8) buffer VxOut { float vxOut[]; };
layout(std430, binding = 9) buffer VyOut { float vyOut[]; };
layout(std430, binding = 10) readonly buffer Conductivity { int cond[]; };
layout(std430, binding = 12) readonly buffer Capacity { int cap[]; };
uniform float u_dt, u_buoyancyK, u_gravityStrength;
uniform float u_damping;
uniform float u_velocityDiffusion, u_velocityCutoff;
uniform float u_condSky, u_condGround;
uniform float u_coreTemp, u_spaceTemp;
uniform int u_worldWidth, u_worldHeight;

float round5(float v) { return floor(v * 100000.0 + 0.5) / 100000.0; }

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    bool solid = s[idx] != 0;
    float capacity = max(float(cap[idx]), 1.0) / 3200.0;
    float capFinal = capacity;
    float tA = tIn[idx];
    float myCond = float(cond[idx]) / 1000.0;
    float flux = 0.0;
    uint leftX = (x == 0u) ? uint(u_worldWidth - 1) : (x - 1u);
    uint rightX = (x == uint(u_worldWidth - 1)) ? 0u : (x + 1u);
    int idxL = int(y * u_worldWidth + leftX);
    int idxR = int(y * u_worldWidth + rightX);
    float condL = float(cond[idxL]) / 1000.0;
    float condR = float(cond[idxR]) / 1000.0;
    float effCondL = min(myCond, condL);
    float effCondR = min(myCond, condR);
    flux += effCondL * (tIn[idxL] - tA);
    flux += effCondR * (tIn[idxR] - tA);

    if (y > 0u) {
        int idxD = int(y - 1u) * u_worldWidth + int(x);
        float condD = float(cond[idxD]) / 1000.0;
        float effCondD = min(myCond, condD);
        flux += effCondD * (tIn[idxD] - tA);
    } else {
        flux += u_condGround * (u_coreTemp - tA);
    }

    if (y < uint(u_worldHeight - 1)) {
        int idxU = int(y + 1u) * u_worldWidth + int(x);
        float condU = float(cond[idxU]) / 1000.0;
        float effCondU = min(myCond, condU);
        flux += effCondU * (tIn[idxU] - tA);
    } else {
        flux += u_condSky * (u_spaceTemp - tA);
    }

    float newT = tA + flux * u_dt / capFinal;
    tOut[idx] = round5(newT);

    if (m[idx] != 0) { vxOut[idx] = 0.0; vyOut[idx] = 0.0; return; }

    float nvx, nvy;
    if (u_velocityDiffusion <= 0.0) {
        nvx = vxIn[idx];
        nvy = vyIn[idx] + newT * u_buoyancyK;
        nvx *= u_damping; nvy *= u_damping;
    } else {
        float vxSum = vxIn[idx]; float vySum = vyIn[idx]; float vCount = 1.0;
        if (m[idxL] == 0) { vxSum += vxIn[idxL] * u_velocityDiffusion; vySum += vyIn[idxL] * u_velocityDiffusion; vCount += u_velocityDiffusion; }
        if (m[idxR] == 0) { vxSum += vxIn[idxR] * u_velocityDiffusion; vySum += vyIn[idxR] * u_velocityDiffusion; vCount += u_velocityDiffusion; }
        if (y > 0u) { int idxD = int(y-1u)*u_worldWidth+int(x); if (m[idxD]==0) { vxSum += vxIn[idxD]*u_velocityDiffusion; vySum += vyIn[idxD]*u_velocityDiffusion; vCount += u_velocityDiffusion; } }
        if (y < uint(u_worldHeight-1)) { int idxU = int(y+1u)*u_worldWidth+int(x); if (m[idxU]==0) { vxSum += vxIn[idxU]*u_velocityDiffusion; vySum += vyIn[idxU]*u_velocityDiffusion; vCount += u_velocityDiffusion; } }
        float invVCount = 1.0 / vCount;
        nvx = (vxSum * invVCount) * u_damping;
        nvy = ((vySum * invVCount) + newT * u_buoyancyK) * u_damping;
    }
    if (u_gravityStrength != 0.0) nvy += u_gravityStrength;
    if (y > 0u && s[(int(y)-1)*u_worldWidth+int(x)] != 0 && nvy < 0.0) {
        float fallSpeed = abs(nvy); nvy = 0.0;
        float splashBoost = fallSpeed * 1.8;
        if (abs(nvx) >= u_velocityCutoff) nvx += (nvx > 0.0 ? splashBoost : -splashBoost);
    }
    vxOut[idx] = nvx; vyOut[idx] = nvy;
}