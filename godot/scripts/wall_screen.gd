class_name WallScreen
extends Node2D

var target_polygons: Array[PackedVector2Array] = []
var shadow_polygons_detailed: Array[Dictionary] = []
var current_iou: float = 0.0

const COLOR_WALL_BG: Color = Color(0.91, 0.85, 0.74, 1.0)
const COLOR_WALL_BORDER: Color = Color(0.38, 0.28, 0.18, 0.85)
const COLOR_TARGET_FILL: Color = Color(0.92, 0.74, 0.32, 0.25)
const COLOR_TARGET_STROKE: Color = Color(0.96, 0.80, 0.38, 0.85)
const COLOR_SHADOW_BASE: Color = Color(0.055, 0.027, 0.012, 1.0)

var _target_material: ShaderMaterial
var _bg_layer: Node2D
var _target_layer: Node2D
var _shadow_layer: Node2D

func _ready():
	# 1. Parchment background layer
	_bg_layer = Node2D.new()
	_bg_layer.name = "BgLayer"
	_bg_layer.draw.connect(_draw_bg)
	add_child(_bg_layer)

	# 2. Golden stencil layer with breathing gold shader
	_target_layer = Node2D.new()
	_target_layer.name = "TargetLayer"
	var shader = load("res://shaders/gold_stencil.gdshader") as Shader
	if shader:
		_target_material = ShaderMaterial.new()
		_target_material.shader = shader
		_target_material.set_shader_parameter("resonance", 0.0)
		_target_layer.material = _target_material
	_target_layer.draw.connect(_draw_target)
	add_child(_target_layer)

	# 3. Soft shadow projection layer
	_shadow_layer = Node2D.new()
	_shadow_layer.name = "ShadowLayer"
	_shadow_layer.draw.connect(_draw_shadows)
	add_child(_shadow_layer)

func set_target(polys: Array[PackedVector2Array]):
	target_polygons = polys
	if _target_layer:
		_target_layer.queue_redraw()

func update_shadows(detailed: Array[Dictionary], iou: float):
	shadow_polygons_detailed = detailed
	current_iou = iou
	if _target_material:
		_target_material.set_shader_parameter("resonance", current_iou)
	if _target_layer:
		_target_layer.queue_redraw()
	if _shadow_layer:
		_shadow_layer.queue_redraw()

func _draw_bg():
	var wall_rect = Rect2(
		ShadowCalculator.WALL_LEFT,
		ShadowCalculator.WALL_TOP,
		ShadowCalculator.WALL_RIGHT - ShadowCalculator.WALL_LEFT,
		ShadowCalculator.WALL_BOTTOM - ShadowCalculator.WALL_TOP
	)

	# Warm parchment vellum surface
	_bg_layer.draw_rect(wall_rect, COLOR_WALL_BG)

	# Subtle top vignette falloff across vellum
	var vellum_header = Rect2(wall_rect.position.x, wall_rect.position.y, wall_rect.size.x, 32.0)
	_bg_layer.draw_rect(vellum_header, Color(0.1, 0.06, 0.03, 0.12))

	# Wall border frame (turned scholar moulding)
	_bg_layer.draw_rect(wall_rect, COLOR_WALL_BORDER, false, 3.0)

	# Delicate corner rivets / marks
	var r_sz: float = 8.0
	_bg_layer.draw_rect(Rect2(wall_rect.position.x + 4, wall_rect.position.y + 4, r_sz, r_sz), Color(0.4, 0.3, 0.2, 0.5))
	_bg_layer.draw_rect(Rect2(wall_rect.end.x - 12, wall_rect.position.y + 4, r_sz, r_sz), Color(0.4, 0.3, 0.2, 0.5))
	_bg_layer.draw_rect(Rect2(wall_rect.position.x + 4, wall_rect.end.y - 12, r_sz, r_sz), Color(0.4, 0.3, 0.2, 0.5))
	_bg_layer.draw_rect(Rect2(wall_rect.end.x - 12, wall_rect.end.y - 12, r_sz, r_sz), Color(0.4, 0.3, 0.2, 0.5))

func _draw_target():
	for poly in target_polygons:
		if poly.size() >= 3:
			_target_layer.draw_colored_polygon(poly, COLOR_TARGET_FILL)
			_target_layer.draw_polyline(poly, COLOR_TARGET_STROKE, 2.5, true)

func _draw_shadows():
	# Projected shadows with soft penumbra
	# Outer soft band first
	for s in shadow_polygons_detailed:
		var outer: PackedVector2Array = s.get("outer", PackedVector2Array())
		if outer.size() >= 3:
			_shadow_layer.draw_colored_polygon(outer, Color(COLOR_SHADOW_BASE.r, COLOR_SHADOW_BASE.g, COLOR_SHADOW_BASE.b, 0.14))

	# Mid band second
	for s in shadow_polygons_detailed:
		var mid: PackedVector2Array = s.get("mid", PackedVector2Array())
		if mid.size() >= 3:
			_shadow_layer.draw_colored_polygon(mid, Color(COLOR_SHADOW_BASE.r, COLOR_SHADOW_BASE.g, COLOR_SHADOW_BASE.b, 0.24))

	# Core sharp silhouette last
	for s in shadow_polygons_detailed:
		var core: PackedVector2Array = s.get("core", PackedVector2Array())
		if core.size() >= 3:
			_shadow_layer.draw_colored_polygon(core, Color(COLOR_SHADOW_BASE.r, COLOR_SHADOW_BASE.g, COLOR_SHADOW_BASE.b, 0.90))

	# Resonance bloom edge when reaching completion
	if current_iou >= 0.85:
		for poly in target_polygons:
			if poly.size() >= 3:
				var bloom_col: Color = Color(1.0, 0.85, 0.4, 0.45 + sin(Time.get_ticks_msec() * 0.008) * 0.2)
				_shadow_layer.draw_polyline(poly, bloom_col, 4.0, true)
