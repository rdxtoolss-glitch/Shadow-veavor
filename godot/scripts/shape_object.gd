class_name ShapeObject
extends RefCounted

enum Kind {
	CIRCLE,
	TRIANGLE,
	SQUARE,
	RECT,
	LINE
}

const CIRCLE_SEGMENTS: int = 48

var kind: Kind = Kind.CIRCLE
var width: float = 100.0
var height: float = 100.0
var x: float = 500.0
var y: float = 600.0
var rotation: float = 0.0 # degrees clockwise

var target_x: float = 500.0
var target_y: float = 600.0
var target_rotation: float = 0.0
var vx: float = 0.0
var vy: float = 0.0
var angular_velocity: float = 0.0
var spin: float = 0.0
var mass: float = 1.0

var tilt_x: float = 0.0
var tilt_y: float = 0.0

var elevation: float = 0.0
var target_elevation: float = 0.0
var is_dragging: bool = false
var is_rotating: bool = false
var is_snapping: bool = false
var proximity_resonance: float = 0.0

func _init(p_kind: Kind = Kind.CIRCLE, p_w: float = 100.0, p_h: float = 100.0, p_x: float = 500.0, p_y: float = 600.0, p_rot: float = 0.0):
	kind = p_kind
	width = p_w
	height = p_h
	x = p_x
	y = p_y
	rotation = p_rot
	target_x = p_x
	target_y = p_y
	target_rotation = p_rot

static func normalize_angle(deg: float) -> float:
	var a: float = fmod(deg, 360.0)
	if a < 0.0: a += 360.0
	return a

static func normalize_delta_angle(diff: float) -> float:
	var a: float = fmod(diff, 360.0)
	if a > 180.0: a -= 360.0
	if a < -180.0: a += 360.0
	return a

static func compute_angular_difference(p_kind: Kind, rot_a: float, rot_b: float) -> float:
	if p_kind == Kind.CIRCLE:
		return 0.0
	var diff: float = absf(normalize_delta_angle(rot_a - rot_b))
	if p_kind == Kind.SQUARE:
		diff = fmod(diff, 90.0)
		if diff > 45.0: diff = 90.0 - diff
	elif p_kind == Kind.RECT or p_kind == Kind.LINE:
		diff = fmod(diff, 180.0)
		if diff > 90.0: diff = 180.0 - diff
	return diff

static func closest_symmetric_angle(p_kind: Kind, current_rot: float, sol_rot: float) -> float:
	if p_kind == Kind.CIRCLE:
		return current_rot
	if p_kind == Kind.SQUARE:
		var best: float = sol_rot
		var min_diff: float = 360.0
		for k in range(4):
			var cand: float = normalize_angle(sol_rot + k * 90.0)
			var diff: float = absf(normalize_delta_angle(current_rot - cand))
			if diff < min_diff:
				min_diff = diff
				best = cand
		return best
	if p_kind == Kind.RECT or p_kind == Kind.LINE:
		var cand0: float = normalize_angle(sol_rot)
		var cand1: float = normalize_angle(sol_rot + 180.0)
		var diff0: float = absf(normalize_delta_angle(current_rot - cand0))
		var diff1: float = absf(normalize_delta_angle(current_rot - cand1))
		return cand0 if diff0 <= diff1 else cand1
	return sol_rot

func snap_to(sol_x: float, sol_y: float, sol_rot: float) -> void:
	target_x = sol_x
	target_y = sol_y
	target_rotation = closest_symmetric_angle(kind, rotation, sol_rot)
	is_snapping = true
	is_dragging = false
	is_rotating = false

func update_elevation(dt: float) -> void:
	var speed: float = 16.0
	elevation += (target_elevation - elevation) * minf(1.0, dt * speed)

func step_physics(dt: float) -> void:
	if dt <= 0.0: return
	if dt > 0.05: dt = 0.05

	if is_dragging:
		x = target_x
		y = target_y
		vx *= pow(0.01, dt)
		vy *= pow(0.01, dt)
		var desired_tilt_x: float = -vy * 0.035
		var desired_tilt_y: float = vx * 0.035
		tilt_x += (desired_tilt_x - tilt_x) * minf(1.0, dt * 20.0)
		tilt_y += (desired_tilt_y - tilt_y) * minf(1.0, dt * 20.0)
	elif is_snapping:
		var dx: float = target_x - x
		var dy: float = target_y - y
		var k: float = 280.0
		var c: float = 28.0
		var fx: float = k * dx - c * vx
		var fy: float = k * dy - c * vy
		vx += (fx / mass) * dt
		vy += (fy / mass) * dt
		x += vx * dt
		y += vy * dt

		if absf(dx) < 0.2 and absf(dy) < 0.2 and absf(vx) < 0.5 and absf(vy) < 0.5:
			x = target_x
			y = target_y
			vx = 0.0
			vy = 0.0
			is_snapping = false
		tilt_x *= pow(0.05, dt)
		tilt_y *= pow(0.05, dt)
	else:
		x += vx * dt
		y += vy * dt
		var friction: float = pow(0.005, dt)
		vx *= friction
		vy *= friction
		if absf(vx) < 0.5: vx = 0.0
		if absf(vy) < 0.5: vy = 0.0
		tilt_x *= pow(0.05, dt)
		tilt_y *= pow(0.05, dt)

		if spin != 0.0:
			rotation = normalize_angle(rotation + spin * dt)

	# Clamped boundary collision
	var min_x: float = ShadowCalculator.STAGE_LEFT
	var max_x: float = ShadowCalculator.STAGE_RIGHT
	var min_y: float = ShadowCalculator.STAGE_TOP
	var max_y: float = ShadowCalculator.STAGE_BOTTOM
	var restitution: float = 0.28

	if x < min_x:
		x = min_x
		vx = absf(vx) * restitution
	elif x > max_x:
		x = max_x
		vx = -absf(vx) * restitution

	if y < min_y:
		y = min_y
		vy = absf(vy) * restitution
	elif y > max_y:
		y = max_y
		vy = -absf(vy) * restitution

	if is_rotating:
		rotation = target_rotation
		angular_velocity = 0.0
	elif is_snapping:
		var d_rot: float = normalize_delta_angle(target_rotation - rotation)
		var k_rot: float = 240.0
		var c_rot: float = 24.0
		var torque: float = k_rot * d_rot - c_rot * angular_velocity
		angular_velocity += torque * dt
		rotation = normalize_angle(rotation + angular_velocity * dt)
		if absf(d_rot) < 0.2 and absf(angular_velocity) < 0.5:
			rotation = target_rotation
			angular_velocity = 0.0
			is_snapping = false
	elif angular_velocity != 0.0:
		rotation = normalize_angle(rotation + angular_velocity * dt)
		angular_velocity *= pow(0.12, dt)
		if absf(angular_velocity) < 0.4: angular_velocity = 0.0

	update_elevation(dt)

func local_silhouette() -> PackedVector2Array:
	var pts: PackedVector2Array = PackedVector2Array()
	match kind:
		Kind.CIRCLE:
			pts.resize(CIRCLE_SEGMENTS)
			var r: float = width * 0.5
			for i in range(CIRCLE_SEGMENTS):
				var a: float = 2.0 * PI * float(i) / float(CIRCLE_SEGMENTS)
				pts[i] = Vector2(r * cos(a), -r * sin(a))
		Kind.TRIANGLE:
			pts.resize(3)
			pts[0] = Vector2(0.0, -height * 0.5)
			pts[1] = Vector2(width * 0.5, height * 0.5)
			pts[2] = Vector2(-width * 0.5, height * 0.5)
		Kind.SQUARE:
			var hw: float = width * 0.5
			pts.resize(4)
			pts[0] = Vector2(-hw, -hw)
			pts[1] = Vector2(hw, -hw)
			pts[2] = Vector2(hw, hw)
			pts[3] = Vector2(-hw, hw)
		Kind.RECT, Kind.LINE:
			var hw: float = width * 0.5
			var hh: float = height * 0.5
			pts.resize(4)
			pts[0] = Vector2(-hw, -hh)
			pts[1] = Vector2(hw, -hh)
			pts[2] = Vector2(hw, hh)
			pts[3] = Vector2(-hw, hh)
	return pts

func apply_transform() -> PackedVector2Array:
	var local: PackedVector2Array = local_silhouette()
	var rad: float = deg_to_rad(rotation)
	var cos_r: float = cos(rad)
	var sin_r: float = sin(rad)
	var out: PackedVector2Array = PackedVector2Array()
	out.resize(local.size())
	for i in range(local.size()):
		var lx: float = local[i].x
		var ly: float = local[i].y
		out[i] = Vector2(lx * cos_r - ly * sin_r + x, lx * sin_r + ly * cos_r + y)
	return out
