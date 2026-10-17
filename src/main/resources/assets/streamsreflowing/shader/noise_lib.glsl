const vec3 GRAD[16] = vec3[](
  vec3(1,1,0), vec3(-1,1,0), vec3(1,-1,0), vec3(-1,-1,0),
  vec3(1,0,1), vec3(-1,0,1), vec3(1,0,-1), vec3(-1,0,-1),
  vec3(0,1,1), vec3(0,-1,1), vec3(0,1,-1), vec3(0,-1,-1),
  vec3(1,1,0), vec3(0,-1,1), vec3(-1,1,0), vec3(0,-1,-1));

float wrap(float v) {
  return v - floor(v / 33554432.0 + 0.5) * 33554432.0;
}
float squeeze(float v) {
  float c = clamp(v, -1.0, 1.0);
  return c / 2.0 - c * c * c / 24.0;
}
float rarity1(float d) {
  return d < -0.5 ? 0.75 : (d < 0.0 ? 1.0 : (d < 0.5 ? 1.5 : 2.0));
}
float rarity2(float d) {
  return d < -0.75 ? 0.5 : (d < -0.5 ? 0.75 : (d < 0.5 ? 1.0 : (d < 0.75 ? 2.0 : 3.0)));
}
float gradDot(int hash, float x, float y, float z) {
  vec3 g = GRAD[hash & 15];
  return g.x * x + g.y * y + g.z * z;
}
float smoothf(float d) {
  return d * d * d * (d * (d * 6.0 - 15.0) + 10.0);
}
float improved(int permOff, float xo, float yo, float zo, float x, float y, float z) {
  float a = x + xo; float b = y + yo; float c = z + zo;
  int i = int(floor(a)); int j = int(floor(b)); int k = int(floor(c));
  float d = a - float(i); float e = b - float(j); float f = c - float(k);
  int pi  = perm[permOff + (i & 255)];
  int pi1 = perm[permOff + ((i + 1) & 255)];
  int pj  = perm[permOff + ((pi + j) & 255)];
  int pj1 = perm[permOff + ((pi + j + 1) & 255)];
  int qj  = perm[permOff + ((pi1 + j) & 255)];
  int qj1 = perm[permOff + ((pi1 + j + 1) & 255)];
  float g000 = gradDot(perm[permOff + ((pj  + k) & 255)], d,       e,       f);
  float g100 = gradDot(perm[permOff + ((qj  + k) & 255)], d - 1.0, e,       f);
  float g010 = gradDot(perm[permOff + ((pj1 + k) & 255)], d,       e - 1.0, f);
  float g110 = gradDot(perm[permOff + ((qj1 + k) & 255)], d - 1.0, e - 1.0, f);
  float g001 = gradDot(perm[permOff + ((pj  + k + 1) & 255)], d,       e,       f - 1.0);
  float g101 = gradDot(perm[permOff + ((qj  + k + 1) & 255)], d - 1.0, e,       f - 1.0);
  float g011 = gradDot(perm[permOff + ((pj1 + k + 1) & 255)], d,       e - 1.0, f - 1.0);
  float g111 = gradDot(perm[permOff + ((qj1 + k + 1) & 255)], d - 1.0, e - 1.0, f - 1.0);
  float u = smoothf(d); float v = smoothf(e); float w = smoothf(f);
  return mix(mix(mix(g000, g100, u), mix(g010, g110, u), v),
             mix(mix(g001, g101, u), mix(g011, g111, u), v), w);
}
// Smeared (5-arg) improved noise: quantized y offset for the grid, original y for the smoothstep.
float improvedS(int permOff, float xo, float yo, float zo,
                float x, float y, float z, float ysc, float ymax) {
  float a = x + xo; float b = y + yo; float c = z + zo;
  int i = int(floor(a)); int j = int(floor(b)); int k = int(floor(c));
  float d = a - float(i); float e = b - float(j); float f = c - float(k);
  float g = 0.0;
  if (ysc != 0.0) {
    float m = (ymax >= 0.0 && ymax < e) ? ymax : e;
    g = floor(m / ysc + 1.0e-7) * ysc;
  }
  float e2 = e - g;
  int pi  = perm[permOff + (i & 255)];
  int pi1 = perm[permOff + ((i + 1) & 255)];
  int pj  = perm[permOff + ((pi + j) & 255)];
  int pj1 = perm[permOff + ((pi + j + 1) & 255)];
  int qj  = perm[permOff + ((pi1 + j) & 255)];
  int qj1 = perm[permOff + ((pi1 + j + 1) & 255)];
  float g000 = gradDot(perm[permOff + ((pj  + k) & 255)], d,       e2,       f);
  float g100 = gradDot(perm[permOff + ((qj  + k) & 255)], d - 1.0, e2,       f);
  float g010 = gradDot(perm[permOff + ((pj1 + k) & 255)], d,       e2 - 1.0, f);
  float g110 = gradDot(perm[permOff + ((qj1 + k) & 255)], d - 1.0, e2 - 1.0, f);
  float g001 = gradDot(perm[permOff + ((pj  + k + 1) & 255)], d,       e2,       f - 1.0);
  float g101 = gradDot(perm[permOff + ((qj  + k + 1) & 255)], d - 1.0, e2,       f - 1.0);
  float g011 = gradDot(perm[permOff + ((pj1 + k + 1) & 255)], d,       e2 - 1.0, f - 1.0);
  float g111 = gradDot(perm[permOff + ((qj1 + k + 1) & 255)], d - 1.0, e2 - 1.0, f - 1.0);
  float u = smoothf(d); float v = smoothf(e); float w = smoothf(f);
  return mix(mix(mix(g000, g100, u), mix(g010, g110, u), v),
             mix(mix(g001, g101, u), mix(g011, g111, u), v), w);
}

