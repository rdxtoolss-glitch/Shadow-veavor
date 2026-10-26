class_name WallScreen
extends Node2D

var target_polygons: Array[PackedVector2Array] = []
var shadow_polygons_detailed: Array[Dictionary] = []
var current_iou: float = 0.0

const COLOR_WALL_BG: Color = Color(0.91, 0.85, 0.74, 1.0)
const COLOR_WALL_BORDER: Color = Color(0.38, 0.28, 0.18, 0.8)
const COLOR_TARGET_FILL: Color = Color(0.85, 0.65, 0.25, 0.12)
const COLOR_TARGET_STROKE: Color = Color(0.92, 0.72, 0.30, 0.65)
const COLOR_SHADOW_BASE: Color = Color(0.055, 0.027, 0.012, 1.0)

func set_target(polys: Array[PackedVector2Array]):
	target_polygons = polys
	queue_redraw()

func update_shadows(detailed: Array[Dictionary], iou: float):
	shadow_polygons_detailed = detailed
	current_iou = iou
	queue_redraw()

func _draw():
	var wall_rect = Rect2(
		ShadowCalculator.WALL_LEFT,
		ShadowCalculator.WALL_TOP,
		ShadowCalculator.WALL_RIGHT - ShadowCalculator.WALL_LEFT,
		ShadowCalculator.WALL_BOTTOM - ShadowCalculator.WALL_TOP
	)

	# 1. Parchment vellum surface
	draw_rect(wall_rect, COLOR_WALL_BG)

	# Subtle top shadow falloff across vellum
	var vellum_header = Rect2(wall_rect.position.x, wall_rect.position.y, wall_rect.size.x, 32.0)
	draw_rect(vellum_header, Color(0.1, 0.06, 0.03, 0.12))

	# Wall border frame
	draw_rect(wall_rect, COLOR_WALL_BORDER, false, 2.5)

	# 2. Target silhouette (the parchment stencil stencil)
	for poly in target_polygons:
		if poly.size() >= 3:
			draw_colored_polygon(poly, COLOR_TARGET_FILL)
			draw_polyline(poly, COLOR_TARGET_STROKE, 2.0, true)

	# 3. Projected shadows with true multi-layer soft penumbra
	# Outer soft band first
	for s in shadow_polygons_detailed:
		var outer: PackedVector2Array = s["outer"]
		if outer.size() >= 3:
			draw_colored_polygon(outer, Color(COLOR_SHADOW_BASE.r, COLOR_SHADOW_BASE.g, COLOR_SHADOW_BASE.b, 0.12))

	# Mid band second
	for s in shadow_polygons_detailed:
		var mid: PackedVector2Array = s["mid"]
		if mid.size() >= 3:
			draw_colored_polygon(mid, Color(COLOR_SHADOW_BASE.r, COLOR_SHADOW_BASE.g, COLOR_SHADOW_BASE.b, 0.20))

	# Core sharp silhouette last
	for s in shadow_polygons_detailed:
		var core: PackedVector2Array = s["core"]
		if core.size() >= 3:
			draw_colored_polygon(core, Color(COLOR_SHADOW_BASE.r, COLOR_SHADOW_BASE.g, COLOR_SHADOW_BASE.b, 0.88))

	# 4. Resonance bloom when reaching completion
	if current_iou >= 0.85:
		for poly in target_polygons:
			if poly.size() >= 3:
				var bloom_col: Color = Color(1.0, 0.85, 0.4, 0.35 + sin(Time.get_ticks_msec() * 0.008) * 0.15)
				draw_polyline(poly, bloom_col, 4.0, true)
