class_name ShadowCalculator
extends RefCounted

## Virtual scene is 1000 x 1600 units
const SCENE_W: float = 1000.0
const SCENE_H: float = 1600.0

## Candle position (light source)
const LIGHT_X: float = 500.0
const LIGHT_Y: float = 215.0
const LIGHT_Z: float = 1.0

## Stage bounds where pieces can be moved
const STAGE_LEFT: float = 120.0
const STAGE_RIGHT: float = 880.0
const STAGE_TOP: float = 290.0
const STAGE_BOTTOM: float = 1050.0

## Wall bounds where shadows land
const WALL_LEFT: float = 40.0
const WALL_RIGHT: float = 960.0
const WALL_TOP: float = 1110.0
const WALL_BOTTOM: float = 1530.0
const WALL_MARGIN: float = 100.0

## Projection tuning
static var perspective_strength: float = 0.55
static var height_near_floor: float = 0.10
static var height_near_candle: float = 0.52
static var vertical_range: float = 220.0
static var rake_strength: float = 1.0
const RAKE_REACH: float = 70.0

## Penumbra edge softness
static var penumbra_base: float = 2.5
static var penumbra_gain: float = 20.0
static var off_axis_softening: float = 3.0
static var penumbra_max: float = 30.0

const LAYER_OUTER: int = 0
const LAYER_MID: int = 1
const LAYER_CORE: int = 2

const LAYER_ALPHA: Array[float] = [30.0 / 255.0, 52.0 / 255.0, 224.0 / 255.0]
const LAYER_SPREAD: Array[float] = [1.0, 0.45, 0.0]
const SHADOW_COLOR: Color = Color(0.055, 0.027, 0.012, 1.0) # #0E0703

static func center_lo() -> float:
	return WALL_TOP + WALL_MARGIN

static func center_hi() -> float:
	return center_lo() + vertical_range

static func height_fraction(shape_y: float) -> float:
	var u: float = (STAGE_BOTTOM - shape_y) / (STAGE_BOTTOM - STAGE_TOP)
	return clampf(u, 0.0, 1.0)

static func height_above_floor(shape_y: float) -> float:
	return height_near_floor + (height_near_candle - height_near_floor) * height_fraction(shape_y)

static func perspective_ratio(shape_y: float) -> float:
	var h: float = height_above_floor(shape_y)
	return LIGHT_Z / (LIGHT_Z - h)

static func scale_for(shape_y: float) -> float:
	var t: float = perspective_ratio(shape_y)
	return 1.0 + perspective_strength * (t - 1.0)

static func rake_for(shape_y: float) -> float:
	return rake_strength * RAKE_REACH * height_above_floor(shape_y)

static func wall_y_for(shape_y: float) -> float:
	return center_lo() + vertical_range * height_fraction(shape_y)

static func wall_x_for(shape_x: float, shape_y: float) -> float:
	return LIGHT_X + scale_for(shape_y) * (shape_x - LIGHT_X) + rake_for(shape_y)

static func penumbra_for(shape_x: float, shape_y: float) -> float:
	var t: float = perspective_ratio(shape_y)
	var off_axis: float = absf(shape_x - LIGHT_X) / maxf(1.0, (WALL_RIGHT - WALL_LEFT))
	var p: float = penumbra_base + penumbra_gain * (t - 1.0) + off_axis_softening * off_axis
	return clampf(p, 0.0, penumbra_max)

static func project(shape: Variant) -> PackedVector2Array:
	var scene_poly: PackedVector2Array = shape.apply_transform()
	var s: float = scale_for(shape.y)
	var wx: float = wall_x_for(shape.x, shape.y)
	var wy: float = wall_y_for(shape.y)
	var cx: float = shape.x
	var cy: float = shape.y

	var out: PackedVector2Array = PackedVector2Array()
	out.resize(scene_poly.size())
	for i in range(scene_poly.size()):
		var v: Vector2 = scene_poly[i]
		out[i] = Vector2(wx + (v.x - cx) * s, wy + (v.y - cy) * s)
	return out

static func project_detailed(shape: Variant) -> Dictionary:
	var core_poly: PackedVector2Array = project(shape)
	var p: float = penumbra_for(shape.x, shape.y)
	var s: float = scale_for(shape.y)
	var wx: float = wall_x_for(shape.x, shape.y)
	var wy: float = wall_y_for(shape.y)
	var center: Vector2 = Vector2(wx, wy)

	if p < 0.5:
		return {
			"core": core_poly,
			"mid": core_poly,
			"outer": core_poly,
			"penumbra": 0.0,
			"softness": 0.0,
			"scale": s,
			"center": center
		}

	var mid_poly: PackedVector2Array = offset_convex(core_poly, p * LAYER_SPREAD[LAYER_MID])
	var outer_poly: PackedVector2Array = offset_convex(core_poly, p * LAYER_SPREAD[LAYER_OUTER])
	var softness: float = clampf((p - penumbra_base) / (penumbra_max - penumbra_base), 0.0, 1.0) if penumbra_max > penumbra_base else 0.0

	return {
		"core": core_poly,
		"mid": mid_poly,
		"outer": outer_poly,
		"penumbra": p,
		"softness": softness,
		"scale": s,
		"center": center
	}

static func offset_convex(p: PackedVector2Array, d: float) -> PackedVector2Array:
	var n: int = p.size()
	if n < 3 or d == 0.0:
		return p

	var out: PackedVector2Array = PackedVector2Array()
	out.resize(n)

	for i in range(n):
		var prev: int = (i - 1 + n) % n
		var next: int = (i + 1) % n

		var e1: Vector2 = p[i] - p[prev]
		var e2: Vector2 = p[next] - p[i]

		var l1: float = e1.length()
		var l2: float = e2.length()
		if l1 < 1e-6: l1 = 1.0
		if l2 < 1e-6: l2 = 1.0

		# Outward normal for clockwise y-down is (ey/len, -ex/len)
		var m1: Vector2 = Vector2(e1.y / l1, -e1.x / l1)
		var m2: Vector2 = Vector2(e2.y / l2, -e2.x / l2)

		var b: Vector2 = m1 + m2
		var bl: float = b.length()

		var ox: float = 0.0
		var oy: float = 0.0
		if bl < 1e-4:
			ox = m2.x
			oy = m2.y
		else:
			var nrm: Vector2 = b / bl
			var dot: float = nrm.dot(m2)
			var miter: float = 1.0 / dot if dot > 0.34 else 1.0
			miter = clampf(miter, 1.0, 3.0)
			ox = nrm.x * miter
			oy = nrm.y * miter

		out[i] = Vector2(p[i].x + ox * d, p[i].y + oy * d)

	return out

static func solve_stage_position_for(wall_x: float, wall_y: float) -> Vector2:
	var u: float = (wall_y - center_lo()) / vertical_range
	u = clampf(u, 0.0, 1.0)
	var stage_y: float = STAGE_BOTTOM - u * (STAGE_BOTTOM - STAGE_TOP)
	var s: float = 1.0 + perspective_strength * (perspective_ratio(stage_y) - 1.0)
	var stage_x: float = LIGHT_X + (wall_x - LIGHT_X - rake_for(stage_y)) / s
	return Vector2(stage_x, stage_y)

static func clamp_stage_x(x: float) -> float:
	return clampf(x, STAGE_LEFT, STAGE_RIGHT)

static func clamp_stage_y(y: float) -> float:
	return clampf(y, STAGE_TOP, STAGE_BOTTOM)

static func damp_stage_x(x: float) -> float:
	if x < STAGE_LEFT:
		var over: float = STAGE_LEFT - x
		return STAGE_LEFT - log(1.0 + over * 0.04) * 16.0
	elif x > STAGE_RIGHT:
		var over: float = x - STAGE_RIGHT
		return STAGE_RIGHT + log(1.0 + over * 0.04) * 16.0
	return x

static func damp_stage_y(y: float) -> float:
	if y < STAGE_TOP:
		var over: float = STAGE_TOP - y
		return STAGE_TOP - log(1.0 + over * 0.04) * 16.0
	elif y > STAGE_BOTTOM:
		var over: float = y - STAGE_BOTTOM
		return STAGE_BOTTOM + log(1.0 + over * 0.04) * 16.0
	return y
