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
uniform float u_dt;
uniform int u_worldWidth, u_worldHeight;

float round5(float v) { return floor(v * 100000.0 + 0.5) / 100000.0; }
float wrapX(float x) { return mod(mod(x, float(u_worldWidth)) + float(u_worldWidth), float(u_worldWidth)); }

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    if (s[idx] != 0 || m[idx] != 0) {
        tOut[idx] = (y == u_worldHeight - 1) ? 0.0 : tIn[idx];
        vxOut[idx] = 0.0; vyOut[idx] = 0.0; return;
    }
    float vxH = vxIn[idx], vyH = vyIn[idx];
    if (isnan(vxH) || isinf(vxH)) vxH = 0.0;
    if (isnan(vyH) || isinf(vyH)) vyH = 0.0;
    float rx = float(x) - vxH * u_dt, ry = float(y) - vyH * u_dt;
    ry = clamp(ry, 0.5, float(u_worldHeight) - 0.5);
    rx = wrapX(rx);
    int sampleX = int(mod(rx, float(u_worldWidth)));
    int sampleY = int(clamp(ry, 0.0, float(u_worldHeight) - 1.0));
    int horzIdx = int(y) * u_worldWidth + sampleX; if (s[horzIdx] != 0) rx = float(x);
    int vertIdx = sampleY * u_worldWidth + int(x); if (s[vertIdx] != 0) ry = float(y);
    int finalSampleX = int(mod(rx, float(u_worldWidth)));
    int finalSampleY = int(clamp(ry, 0.0, float(u_worldHeight) - 1.0));
    int finalIdx = finalSampleY * u_worldWidth + finalSampleX;
    if (s[finalIdx] != 0) { rx = float(x); ry = float(y); }

    if (ry > float(u_worldHeight) - 1.0) {
        tOut[idx] = 0.0;
        vxOut[idx] = 0.0;
        vyOut[idx] = 0.0;
        return;
    }

    int x0 = int(floor(rx)), x1 = (x0 + 1) % u_worldWidth;
    int y0 = int(clamp(ry, 0.0, float(u_worldHeight) - 2.0)), y1 = y0 + 1;
    float s1 = rx - float(x0), s0 = 1.0 - s1, t1 = ry - float(y0), t0 = 1.0 - t1;
    x0 = int(mod(float(x0), float(u_worldWidth)));
    int i00 = y0 * u_worldWidth + x0, i01 = y1 * u_worldWidth + x0;
    int i10 = y0 * u_worldWidth + x1, i11 = y1 * u_worldWidth + x1;
    float t00 = (s[i00] != 0) ? tIn[idx] : tIn[i00];
    float t01 = (s[i01] != 0) ? tIn[idx] : tIn[i01];
    float t10 = (s[i10] != 0) ? tIn[idx] : tIn[i10];
    float t11 = (s[i11] != 0) ? tIn[idx] : tIn[i11];
    tOut[idx] = round5(s0 * (t0 * t00 + t1 * t01) + s1 * (t0 * t10 + t1 * t11));
    float vx00 = (s[i00] != 0) ? 0.0 : vxIn[i00], vy00 = (s[i00] != 0) ? 0.0 : vyIn[i00];
    float vx01 = (s[i01] != 0) ? 0.0 : vxIn[i01], vy01 = (s[i01] != 0) ? 0.0 : vyIn[i01];
    float vx10 = (s[i10] != 0) ? 0.0 : vxIn[i10], vy10 = (s[i10] != 0) ? 0.0 : vyIn[i10];
    float vx11 = (s[i11] != 0) ? 0.0 : vxIn[i11], vy11 = (s[i11] != 0) ? 0.0 : vyIn[i11];
    vxOut[idx] = (s0 * (t0 * vx00 + t1 * vx01) + s1 * (t0 * vx10 + t1 * vx11));
    vyOut[idx] = (s0 * (t0 * vy00 + t1 * vy01) + s1 * (t0 * vy10 + t1 * vy11));
}