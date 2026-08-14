#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 0) readonly buffer TIn { float tIn[]; };
layout(std430, binding = 1) writeonly buffer TOut { float tOut[]; };
layout(std430, binding = 2) readonly buffer Solid { int solid[]; };
layout(std430, binding = 3) readonly buffer Surfaces { int surf[]; };
uniform float u_coolingRate;
uniform float u_dt;
uniform float u_spaceTemp;
uniform int u_worldWidth, u_worldHeight;

float round5(float v) { return floor(v * 100000.0 + 0.5) / 100000.0; }

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    float isAir = 1.0 - float(solid[idx]);
    if (isAir < 1.0 || int(y) < surf[x]) {
        tOut[idx] = tIn[idx];
        return;
    }
    float temp = tIn[idx];
    float h = float(y) / max(1.0, float(u_worldHeight - 1));
    float heightFactor = h * h;
    float cooling = u_coolingRate * heightFactor * (temp - u_spaceTemp) * u_dt * isAir;
    tOut[idx] = round5(max(temp - cooling, u_spaceTemp));
}