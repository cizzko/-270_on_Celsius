#version 460 core
layout(local_size_x = 256) in;
layout(std430, binding = 0) buffer Temps { float t[]; };
layout(std430, binding = 1) readonly buffer Surfaces { int surf[]; };
layout(std430, binding = 2) readonly buffer Solid { int s[]; };
layout(std430, binding = 3) readonly buffer Emissivity { int emis[]; };
uniform float u_coolingRate;
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
    float emissivity = float(emis[idx]) / 100.0;
    float temp = t[idx];

    float T_kelvin = max(temp + 270.0, 1.0);
    float T2 = T_kelvin * T_kelvin;
    float T4 = T2 * T2;
    float T_ref = 313.0;
    float T_ref4 = T_ref * T_ref * T_ref * T_ref;
    float normalized = T4 / T_ref4;

    float cooling = u_coolingRate * emissivity * normalized * u_dt * solid * valid;
    t[idx] = round5(max(temp - cooling, -270.0));
}