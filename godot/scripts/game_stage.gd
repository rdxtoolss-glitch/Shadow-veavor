class_name GameStage
extends Node2D

const COLOR_STAGE_BG: Color = Color(0.12, 0.09, 0.07, 1.0)
const COLOR_STAGE_BORDER: Color = Color(0.28, 0.20, 0.14, 0.6)
const COLOR_BRASS_CORNER: Color = Color(0.75, 0.58, 0.30, 0.8)

var _flicker_phase: float = 0.0

func _process(dt: float):
	_flicker_phase += dt * 5.0
	queue_redraw()

func _draw():
	var stage_rect = Rect2(
		ShadowCalculator.STAGE_LEFT,
		ShadowCalculator.STAGE_TOP,
		ShadowCalculator.STAGE_RIGHT - ShadowCalculator.STAGE_LEFT,
		ShadowCalculator.STAGE_BOTTOM - ShadowCalculator.STAGE_TOP
	)

	# 1. Dark walnut workbench wood
	draw_rect(stage_rect, COLOR_STAGE_BG)
	draw_rect(stage_rect, COLOR_STAGE_BORDER, false, 2.0)

	# 2. Stage brass corner accents
	var corners = [
		stage_rect.position,
		Vector2(stage_rect.end.x, stage_rect.position.y),
		Vector2(stage_rect.position.x, stage_rect.end.y),
		stage_rect.end
	]
	for c in corners:
		draw_circle(c, 5.0, COLOR_BRASS_CORNER)

	# 3. Candle assembly at (500, 215)
	var lx: float = ShadowCalculator.LIGHT_X
	var ly: float = ShadowCalculator.LIGHT_Y

	# Brass holder base
	draw_rect(Rect2(lx - 28.0, ly + 25.0, 56.0, 10.0), COLOR_BRASS_CORNER)
	draw_rect(Rect2(lx - 12.0, ly + 14.0, 24.0, 12.0), Color(0.65, 0.50, 0.25, 1.0))

	# Wax candle column
	draw_rect(Rect2(lx - 8.0, ly, 16.0, 16.0), Color(0.92, 0.88, 0.80, 1.0))
	# Wick
	draw_line(Vector2(lx, ly), Vector2(lx, ly - 7.0), Color(0.1, 0.1, 0.1, 1.0), 2.0)

	# Glowing animated flame with dynamic flicker
	var flick: float = sin(_flicker_phase) * 1.5 + sin(_flicker_phase * 2.3) * 0.8
	var flame_tip: Vector2 = Vector2(lx + flick * 0.5, ly - 22.0)
	var flame_left: Vector2 = Vector2(lx - 6.0, ly - 9.0)
	var flame_right: Vector2 = Vector2(lx + 6.0, ly - 9.0)
	var flame_poly: PackedVector2Array = [Vector2(lx, ly - 7.0), flame_left, flame_tip, flame_right]

	# Outer amber glow halo
	draw_circle(Vector2(lx, ly - 10.0), 28.0 + flick, Color(1.0, 0.65, 0.2, 0.18))
	draw_circle(Vector2(lx, ly - 10.0), 16.0 + flick * 0.5, Color(1.0, 0.80, 0.3, 0.32))

	# Inner bright flame core
	draw_colored_polygon(flame_poly, Color(1.0, 0.55, 0.1, 0.95))
	draw_circle(Vector2(lx, ly - 11.0), 4.0, Color(1.0, 0.95, 0.85, 1.0))
