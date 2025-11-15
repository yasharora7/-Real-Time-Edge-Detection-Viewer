precision mediump float;
uniform sampler2D uTexture;
varying vec2 vTexCoord;

void main() {
    float y = texture2D(uTexture, vTexCoord).r;
    gl_FragColor = vec4(y, y, y, 1.0);
}
