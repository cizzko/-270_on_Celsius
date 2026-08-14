#version 460 core
layout(local_size_x = 256) in;
layout(std430, binding = 0) buffer Temps { float t[]; };
layout(std430, binding = 1) readonly buffer Surfaces { int surf[]; };
layout(std430, binding = 2) readonly buffer Solid { int s[]; };
layout(std430, binding = 3) readonly buffer Albedo { int alb[]; };
layout(std430, binding = 4) readonly buffer Capacity { int cap[]; };
uniform float u_solarFlux;
uniform float u_globalTime;
uniform float u_effectiveWidth;
uniform float u_dt;
uniform int u_worldWidth, u_worldHeight;

float round5(float v) { return floor(v * 100000.0 + 0.5) / 100000.0; }

void main() {
    uint x = gl_GlobalInvocationID.x;
    if (x >= u_worldWidth) return;
    int surfY = surf[x];
    float valid = step(1.0, float(surfY)) * step(float(surfY), float(u_worldHeight - 1));
    int blockY = surfY - 1;
    int idx = blockY * u_worldWidth + int(x);
    float solid = float(s[idx]);
    if (solid == 0.0) return;
    float deltaX = u_globalTime - float(x);
    float angle = (deltaX / u_effectiveWidth) * 6.283185307179586;
    float cosAngle = cos(angle);
    float angleFactor = acos(cosAngle) / 3.141592653589793;
    float sunFactor = 1.0 - angleFactor;
    float albedo = float(alb[idx]) / 100.0;
    float heatGain = u_solarFlux * (1.0 - albedo) * sunFactor * u_dt * valid;
    float heatCapacity = max(float(cap[idx]) * 1000.0, 1.0);
    t[idx] = round5(t[idx] + heatGain / heatCapacity);
}