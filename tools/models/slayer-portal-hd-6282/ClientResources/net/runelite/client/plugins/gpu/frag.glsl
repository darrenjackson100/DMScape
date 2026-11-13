/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#version 330

//#define FRAG_UVS
//#define ZBUF

uniform sampler2DArray textures;
uniform sampler2DArray authoredTextures;
uniform float brightness;
uniform float slayerPortalTime;
uniform float smoothBanding;
uniform vec4 fogColor;
uniform int colorBlindMode;
uniform float textureLightMode;

in vec4 fColor;
noperspective centroid in float fHsl;
flat in int fTextureId;
in vec2 fUv;
in float fFogAmount;
#ifdef ZBUF
in float fDepth;
#endif

out vec4 FragColor;

#include "hsl_to_rgb.glsl"
#include "colorblind.glsl"

#ifdef ZBUF
float linear_depth(float depth) {
  // depth is computed as 100/z, solve for z
  float z = 100 / depth;
  return 1 - z / 10000;  // we don't have a far plane, but the client uses 10000
}
#endif

void main() {
  vec4 c;

  if (fTextureId >= 40001 && fTextureId <= 40003) {
    // Authored artwork is full-colour sRGB; avoid the cache palette brightness transform.
    c = texture(authoredTextures, vec3(fUv, float(fTextureId - 40001)));
    c.a *= fColor.a;
  } else if (fTextureId == 41004) {
    vec2 p = (fUv - vec2(0.5,0.47)) * vec2(2.0,2.05);
    float r = length(p); float a = atan(p.y,p.x);
    float phase = slayerPortalTime * 6.2831853 / 48.0;
    float curl = a * 3.0 + log(r + 0.12) * 5.5;
    float body = pow(0.5 + 0.5*sin(curl - phase*3.0), 6.0);
    float mist = pow(0.5 + 0.5*sin(a*4.0 - r*11.0 + phase*4.0), 10.0);
    float mask = smoothstep(0.15,0.44,r)*(1.0-smoothstep(0.72,1.05,r));
    vec3 ink = vec3(0.006,0.002,0.004);
    vec3 blood = vec3(0.27,0.008,0.019)*body + vec3(0.09,0.002,0.006)*mist;
    c = vec4(ink + blood*mask,1.0);
  } else if (fTextureId == 41003) {
    float radius = length((fUv - vec2(0.5)) * 2.0);
    float falloff = pow(max(0.0, 1.0 - radius), 2.4);
    c = vec4(1.0, 0.68, 0.20, falloff * 0.65 * clamp((fColor.a - 7.0/255.0) * 255.0/248.0, 0.0, 1.0));
  } else if (fTextureId == 41002) {
    // Altar pulse surfaces: native alpha animation, zero residual at rest.
    c = vec4(vec3(1.45, 1.12, 0.40) * clamp(fHsl / 110.0, 0.80, 1.0),
      clamp((fColor.a - 7.0/255.0) * 255.0/248.0, 0.0, 1.0));
  } else if (fTextureId == 41001) {
    c = texture(authoredTextures, vec3(fUv, 3.0));
    c.rgb *= clamp(fHsl / 95.0, 0.55, 1.10);
    c.a *= fColor.a;
  } else if (fTextureId > 0) {
    int textureIdx = fTextureId - 1;

    vec4 textureColor = texture(textures, vec3(fUv, float(textureIdx)));
    vec4 textureColorBrightness = pow(textureColor, vec4(brightness, brightness, brightness, 1.0f));

    // textured triangles hsl is a 7 bit lightness 2-126
    float light = fHsl / 127.f;
    vec3 mul = (1.f - textureLightMode) * vec3(light) + textureLightMode * fColor.rgb;
    c = textureColorBrightness * vec4(mul, fColor.a);
  } else {
    // pick interpolated hsl or rgb depending on smooth banding setting
    vec3 rgb = hslToRgb(int(fHsl)) * smoothBanding + fColor.rgb * (1.f - smoothBanding);
    c = vec4(rgb, fColor.a);
  }

  if (colorBlindMode > 0) {
    c.rgb = colorblind(colorBlindMode, c.rgb);
  }

  vec3 mixedColor = mix(c.rgb, fogColor.rgb, fFogAmount);
  FragColor = vec4(mixedColor, c.a);

#ifdef FRAG_UVS
  if (fTextureId > 0) {
    FragColor = vec4(fUv.x, 0, fUv.y, 1);
  }
#endif

#ifdef ZBUF
  float dc = linear_depth(fDepth);
  if (dc > 1.0) {
    FragColor = vec4(1, 0, 0, 1);
  } else if (dc < -1.0) {
    FragColor = vec4(0, 0, 1, 1);
  } else if (dc < 0.0) {
    FragColor = vec4(0, 1, 0, 1);
  } else {
    FragColor = vec4(dc, dc, dc, 1);
  }
#endif
}
