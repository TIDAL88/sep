uniform sampler2D tex;

uniform vec2 centerUV;
uniform float radiusPx;
uniform float screenWidth;
uniform float screenHeight;
uniform float visibleU;
uniform float visibleV;
uniform float intensity;

void main() {
    vec2 texCoord = gl_TexCoord[0].xy;
    vec2 dUV = texCoord - centerUV;

    vec2 dPx = vec2(
        dUV.x * (screenWidth / visibleU),
        dUV.y * (screenHeight / visibleV)
    );

    float distPx = length(dPx);
    vec4 col = texture2D(tex, texCoord);

    if (distPx <= radiusPx) {
        float b = (col.r + col.g + col.b) / 3.0;

        col.r *= 1.0 + 0.12 * intensity;
        col.g *= 1.0 - 0.32 * intensity;
        col.b *= 1.0 - 0.45 * intensity;

        col.r += b * 0.26 * intensity;
        col.g += b * 0.03 * intensity;
        col.b += b * 0.02 * intensity;
    }

    gl_FragColor = col;
}
