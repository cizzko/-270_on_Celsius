#version 460 core
layout(local_size_x = 16, local_size_y = 16) in;
layout(std430, binding = 0) readonly buffer Solid { int s[]; };
layout(std430, binding = 1) readonly buffer Mask { int m[]; };
layout(std430, binding = 20) readonly buffer DensityIn { float dIn[]; };
layout(std430, binding = 3) readonly buffer VxIn { float vxIn[]; };
layout(std430, binding = 4) readonly buffer VyIn { float vyIn[]; };
layout(std430, binding = 21) buffer DensityOut { float dOut[]; };
uniform float u_dt;
uniform int u_worldWidth, u_worldHeight;

float wrapX(float x) { return mod(mod(x, float(u_worldWidth)) + float(u_worldWidth), float(u_worldWidth)); }

void main() {
    uint x = gl_GlobalInvocationID.x, y = gl_GlobalInvocationID.y;
    if (x >= u_worldWidth || y >= u_worldHeight) return;
    int idx = int(y * u_worldWidth + x);
    if (s[idx] != 0 || m[idx] != 0) {
        dOut[idx] = dIn[idx];
        return;
    }
    float vx = vxIn[idx], vy = vyIn[idx];
    if (isnan(vx) || isinf(vx)) vx = 0.0;
    if (isnan(vy) || isinf(vy)) vy = 0.0;
    float rx = float(x) - vx * u_dt;
    float ry = float(y) - vy * u_dt;
    ry = clamp(ry, 0.5, float(u_worldHeight) - 1.5);
    rx = wrapX(rx);
    int sx = int(mod(rx, float(u_worldWidth)));
    int sy = int(clamp(ry, 0.0, float(u_worldHeight) - 1.0));
    int horzIdx = int(y) * u_worldWidth + sx;
    if (s[horzIdx] != 0) rx = float(x);
    int vertIdx = sy * u_worldWidth + int(x);
    if (s[vertIdx] != 0) ry = float(y);
    int finalX = int(mod(rx, float(u_worldWidth)));
    int finalY = int(clamp(ry, 0.0, float(u_worldHeight) - 1.0));
    int finalIdx = finalY * u_worldWidth + finalX;
    if (s[finalIdx] != 0) { rx = float(x); ry = float(y); }
    int x0 = int(floor(rx)), x1 = (x0 + 1) % u_worldWidth;
    int y0 = int(clamp(ry, 0.0, float(u_worldHeight) - 2.0)), y1 = y0 + 1;
    float s1 = rx - float(x0), s0 = 1.0 - s1, t1 = ry - float(y0), t0 = 1.0 - t1;
    x0 = int(mod(float(x0), float(u_worldWidth)));
    int i00 = y0 * u_worldWidth + x0, i01 = y1 * u_worldWidth + x0;
    int i10 = y0 * u_worldWidth + x1, i11 = y1 * u_worldWidth + x1;
    float d00 = (s[i00] != 0) ? dIn[idx] : dIn[i00];
    float d01 = (s[i01] != 0) ? dIn[idx] : dIn[i01];
    float d10 = (s[i10] != 0) ? dIn[idx] : dIn[i10];
    float d11 = (s[i11] != 0) ? dIn[idx] : dIn[i11];
    dOut[idx] = s0 * (t0 * d00 + t1 * d01) + s1 * (t0 * d10 + t1 * d11);
}