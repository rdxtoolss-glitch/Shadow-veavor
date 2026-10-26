class_name ShapeToken
extends Node2D

signal picked(token: ShapeToken)
signal dragged(token: ShapeToken)
signal released(token: ShapeToken)
signal rotated(token: ShapeToken)

var shape_data: ShapeObject
var is_selected: bool = false

var _drag_offset: Vector2 = Vector2.ZERO
var _initial_rot_angle: float = 0.0
var _initial_shape_rot: float = 0.0
var _is_handle_dragging: bool = false

const COLOR_FILL: Color = Color(0.24, 0.17, 0.12, 1.0)
const COLOR_STROKE: Color = Color(0.85, 0.68, 0.38, 0.9)
const COLOR_HIGHLIGHT: Color = Color(0.95, 0.82, 0.55, 0.6)
const COLOR_SHADOW: Color = Color(0.04, 0.02, 0.01, 0.45)

func _ready():
	set_process(true)
	queue_redraw()

func setup(data: ShapeObject):
	shape_data = data
	position = Vector2(data.x, data.y)
	rotation_degrees = data.rotation
	queue_redraw()

func _process(dt: float):
	if not shape_data: return

	shape_data.step_physics(dt)
	position = Vector2(shape_data.x, shape_data.y)
	rotation_degrees = shape_data.rotation
	queue_redraw()

func _draw():
	if not shape_data: return

	var local_pts: PackedVector2Array = shape_data.local_silhouette()
	if local_pts.size() < 3: return

	var elev: float = shape_data.elevation

	# 1. Lift drop shadow
	if elev > 0.01:
		var shadow_offset: Vector2 = Vector2(0.0, 12.0 * elev)
		var shadow_scale: float = 1.0 + elev * 0.06
		var shadow_pts: PackedVector2Array = PackedVector2Array()
		shadow_pts.resize(local_pts.size())
		for i in range(local_pts.size()):
			shadow_pts[i] = local_pts[i] * shadow_scale + shadow_offset
		var shadow_alpha: float = COLOR_SHADOW.a * (1.0 - elev * 0.3)
		draw_colored_polygon(shadow_pts, Color(COLOR_SHADOW.r, COLOR_SHADOW.g, COLOR_SHADOW.b, shadow_alpha))

	# 2. Main token body
	var body_color = COLOR_FILL
	if is_selected:
		body_color = body_color.lerp(Color(0.35, 0.25, 0.18, 1.0), 0.3)
	draw_colored_polygon(local_pts, body_color)

	# 3. Subtle top-left highlight and stroke
	draw_polyline(local_pts, COLOR_STROKE, 2.5, true)

	# 4. Center artisan rivet
	draw_circle(Vector2.ZERO, 3.5, COLOR_STROKE)

	# 5. Non-touchable visual selection ring (does NOT collide or push other objects)
	if is_selected:
		var r: float = shape_data.base_radius() + 24.0
		# Delicate dashed or thin halo
		draw_arc(Vector2.ZERO, r, 0, TAU, 36, Color(COLOR_STROKE.r, COLOR_STROKE.g, COLOR_STROKE.b, 0.35), 1.5)
		# Rotation handle pip at top
		var handle_pos: Vector2 = Vector2(0, -r)
		draw_circle(handle_pos, 7.0, COLOR_STROKE)
		draw_circle(handle_pos, 4.0, Color.WHITE)

func contains_point(global_pt: Vector2) -> bool:
	if not shape_data: return false
	var local_pt: Vector2 = to_local(global_pt)
	return local_pt.length() <= shape_data.base_radius() + 16.0

func is_on_rotation_handle(global_pt: Vector2) -> bool:
	if not is_selected or not shape_data: return false
	var local_pt: Vector2 = to_local(global_pt)
	var r: float = shape_data.base_radius() + 24.0
	var handle_pos: Vector2 = Vector2(0, -r)
	return local_pt.distance_to(handle_pos) <= 24.0

func start_drag(global_pt: Vector2):
	if not shape_data: return
	shape_data.is_dragging = true
	shape_data.target_elevation = 1.0
	_drag_offset = Vector2(shape_data.x, shape_data.y) - global_pt
	_is_handle_dragging = false
	picked.emit(self)

func start_handle_drag(global_pt: Vector2):
	if not shape_data: return
	shape_data.is_rotating = true
	_is_handle_dragging = true
	var delta: Vector2 = global_pt - Vector2(shape_data.x, shape_data.y)
	_initial_rot_angle = rad_to_deg(delta.angle())
	_initial_shape_rot = shape_data.rotation
	picked.emit(self)

func update_drag(global_pt: Vector2):
	if not shape_data: return
	if _is_handle_dragging:
		var delta: Vector2 = global_pt - Vector2(shape_data.x, shape_data.y)
		var cur_angle: float = rad_to_deg(delta.angle())
		var d_angle: float = cur_angle - _initial_rot_angle
		var new_rot: float = ShapeObject.normalize_angle(_initial_shape_rot + d_angle)

		# 45-degree snap assist detent
		var detent: float = round(new_rot / 45.0) * 45.0
		if absf(ShapeObject.normalize_delta_angle(new_rot - detent)) < 4.5:
			new_rot = ShapeObject.normalize_angle(detent)

		shape_data.target_rotation = new_rot
		rotated.emit(self)
	elif shape_data.is_dragging:
		# ZERO LAG instant tracking under touch/mouse
		var raw_x: float = global_pt.x + _drag_offset.x
		var raw_y: float = global_pt.y + _drag_offset.y
		shape_data.target_x = ShadowCalculator.damp_stage_x(raw_x)
		shape_data.target_y = ShadowCalculator.damp_stage_y(raw_y)
		dragged.emit(self)

func finish_drag():
	if not shape_data: return
	shape_data.is_dragging = false
	shape_data.is_rotating = false
	shape_data.target_elevation = 0.0
	_is_handle_dragging = false
	released.emit(self)
