precision mediump float;
uniform sampler2D uTex;
varying vec2 vTexCoord;
void main() {
  float y = texture2D(uTex, vTexCoord).r;
  gl_FragColor = vec4(y, y, y, 1.0);
}
