shader_type canvas_item;

// Parchment vellum wall with subtle grain, warm vignette, and candle light falloff
uniform vec4 base_color : source_color = vec4(0.92, 0.86, 0.76, 1.0);
uniform vec4 shadow_tint : source_color = vec4(0.12, 0.08, 0.05, 1.0);
uniform float grain_strength : hint_range(0.0, 0.2) = 0.04;
uniform float vignette_strength : hint_range(0.0, 1.0) = 0.35;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void fragment() {
    vec2 uv = UV;
    
    // Subtle procedural grain
    float noise = hash(uv * 320.0 + vec2(0.12, 0.34)) * 2.0 - 1.0;
    
    // Top-to-bottom warm light gradient from the overhead candle
    float light_falloff = 1.0 - uv.y * 0.18;
    
    // Radial edge vignette
    vec2 center_dist = (uv - 0.5) * 2.0;
    float dist = dot(center_dist, center_dist);
    float vignette = 1.0 - dist * (vignette_strength * 0.4);
    
    vec3 col = base_color.rgb * light_falloff * vignette;
    col += noise * grain_strength;
    
    COLOR = vec4(col, base_color.a);
}
