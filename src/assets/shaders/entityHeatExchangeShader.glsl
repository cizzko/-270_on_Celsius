#version 460 core
layout(local_size_x = 64) in;
layout(std430, binding = 16) readonly buffer EntityIn { float dataIn[]; };
layout(std430, binding = 17) buffer Temps { float temps[]; };
layout(std430, binding = 18) writeonly buffer EntityOut { float delta[]; };
layout(std430, binding = 19) readonly buffer Mask { int mask[]; };

uniform int u_worldWidth, u_worldHeight;
uniform int u_entityCount;
uniform float u_P, u_L, u_S_steep, u_F_steep, u_T_target;
uniform float u_baseFrac, u_bonusFrac, u_overheatRef, u_overheatRange;

float round5(float v) { return floor(v * 100000.0 + 0.5) / 100000.0; }

void main() {
    uint id = gl_GlobalInvocationID.x;
    if (id >= u_entityCount) return;

    uint base = id * 5u;
    int ex = int(dataIn[base+0u]);
    int ey = int(dataIn[base+1u]);
    int radius = int(dataIn[base+2u]);
    radius = min(max(radius, 0), 16);
    float curTemp = dataIn[base+3u];
    float heatTrans = dataIn[base+4u];

    int count = 0;
    float sumTemp = 0.0;

    for (int dx = -radius; dx <= radius; dx++) {
        for (int dy = -radius; dy <= radius; dy++) {
            int sx = ex + dx;
            int sy = ey + dy;
            if (sx >= 0 && sx < u_worldWidth && sy >= 0 && sy < u_worldHeight) {
                int idx = sy * u_worldWidth + sx;
                if (mask[idx] == 0) {
                    sumTemp += temps[idx];
                    count++;
                }
            }
        }
    }

    float overHeatFactor = 1.0 - (curTemp - u_overheatRef) / u_overheatRange;
    float P_dyn = max(0.02, u_P * overHeatFactor);
    float baseMet = u_baseFrac * P_dyn;
    float maxBonus = u_bonusFrac * P_dyn;
    float deltaTarget = u_T_target - curTemp;
    float curveFactor = 1.0 / (1.0 + exp(-u_S_steep * deltaTarget));
    float metabolism = baseMet + (maxBonus * curveFactor);
    float exhaustionFactor = 1.0 / (1.0 + exp(-(curTemp - u_L) * u_F_steep));
    metabolism *= exhaustionFactor;

    if (count == 0) {
        delta[id] = metabolism;
        return;
    }

    float envTemp = sumTemp / float(count);
    float exchange = (envTemp - curTemp) * heatTrans;
    float totalDelta = metabolism + exchange;

    float energyPerCell = -exchange / float(count);

    for (int dx = -radius; dx <= radius; dx++) {
        for (int dy = -radius; dy <= radius; dy++) {
            int sx = ex + dx;
            int sy = ey + dy;
            if (sx >= 0 && sx < u_worldWidth && sy >= 0 && sy < u_worldHeight) {
                int idx = sy * u_worldWidth + sx;
                if (mask[idx] == 0) {
                    temps[idx] = round5(temps[idx] + energyPerCell);
                }
            }
        }
    }

    delta[id] = totalDelta;
}
