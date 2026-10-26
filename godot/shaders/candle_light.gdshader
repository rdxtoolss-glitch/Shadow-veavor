shader_type canvas_item;

uniform vec4 flame_inner : source_color = vec4(1.0, 0.95, 0.82, 1.0);
uniform vec4 flame_outer : source_color = vec4(1.0, 0.55, 0.12, 0.8);
uniform vec4 halo_color : source_color = vec4(1.0, 0.65, 0.20, 0.25);
uniform float flicker_speed : float = 4.5;

void fragment() {
    vec2 uv = (UV - 0.5) * 2.0;
    float dist = length(uv);
    
    float time = TIME * flicker_speed;
    float flicker = sin(time) * 0.05 + sin(time * 2.3) * 0.03 + sin(time * 5.1) * 0.02;
    float effective_dist = dist + flicker;
    
    float halo = clamp(1.0 - effective_dist, 0.0, 1.0);
    halo = pow(halo, 2.2);
    
    vec4 col = mix(halo_color, flame_outer, clamp(1.0 - effective_dist * 2.0, 0.0, 1.0));
    col = mix(col, flame_inner, clamp(1.0 - effective_dist * 4.0, 0.0, 1.0));
    
    COLOR = vec4(col.rgb, col.a * halo);
}
