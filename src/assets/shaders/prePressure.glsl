#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 1) readonly buffer BoundaryMask { int bMask[]; };
layout(std430, binding = 3) readonly buffer VxIn { float vxIn[]; };
layout(std430, binding = 4) readonly buffer VyIn { float vyIn[]; };
layout(std430, binding = 5) buffer Pressure { float pressure[]; };
layout(std430, binding = 6) buffer Divergence { float divergence[]; };
layout(std430, binding = 14) readonly buffer Temps { float t[]; };
layout(std430, binding = 20) readonly buffer Density { float density[]; };
uniform float u_pressureDecay, u_pressureK, u_dt;
uniform float u_thermalExpansion, u_refTemp;
uniform float u_tempScale;
uniform float u_densityK;
uniform int u_worldWidth, u_worldHeight;

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    float isSolid = float(bMask[idx]);
    float cellActive = 1.0 - isSolid;

    pressure[idx] = mix(pressure[idx] * u_pressureDecay, pressure[idx], isSolid);

    int leftX  = (x == 0u ? u_worldWidth - 1 : int(x - 1u));
    int rightX = (x == u_worldWidth - 1u ? 0 : int(x + 1u));
    int idxL = int(y) * u_worldWidth + leftX;
    int idxR = int(y) * u_worldWidth + rightX;
    float vxL = mix(0.0, vxIn[idxL], 1.0 - float(bMask[idxL]));
    float vxR = mix(0.0, vxIn[idxR], 1.0 - float(bMask[idxR]));
    float vyD = mix(0.0, mix(0.0, vyIn[int(y-1u)*u_worldWidth + int(x)], 1.0 - float(bMask[int(y-1u)*u_worldWidth + int(x)])), step(1.0, float(y)));
    float vyU = mix(0.0, mix(0.0, vyIn[int(y+1u)*u_worldWidth + int(x)], 1.0 - float(bMask[int(y+1u)*u_worldWidth + int(x)])), step(float(y), float(u_worldHeight-2)));

    float divVal = -0.5 * ((vxR - vxL) + (vyU - vyD)) * u_pressureK / u_dt;

    float temp = t[idx];
    float delta = (temp - u_refTemp) / max(u_tempScale, 1.0);
    divVal += u_thermalExpansion * u_tempScale * tanh(delta);
    divVal += u_densityK * (density[idx] - 1.0);

    divergence[idx] = mix(0.0, divVal, cellActive);
}