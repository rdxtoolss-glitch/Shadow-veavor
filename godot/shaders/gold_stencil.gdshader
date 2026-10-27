shader_type canvas_item;

uniform vec4 gold_color : source_color = vec4(0.96, 0.78, 0.32, 1.0);
uniform vec4 glow_color : source_color = vec4(1.0, 0.90, 0.55, 1.0);
uniform float resonance : hint_range(0.0, 1.0) = 0.0;
uniform float pulse_speed = 2.5;

void fragment() {
    float t = TIME * pulse_speed;
    float breathing = sin(t) * 0.15 + 0.85;

    // Golden light bleed when alignment is high
    float bleed = pow(resonance, 2.5) * 1.8;
    vec3 col = mix(gold_color.rgb, glow_color.rgb, resonance * breathing);
    col += glow_color.rgb * bleed;

    COLOR = vec4(col, COLOR.a * (0.5 + resonance * 0.5));
}
