class_name SilhouetteMatcher
extends RefCounted

const MATCH_THRESHOLD: float = 0.85
const CELL_SIZE: float = 10.0

var grid_width: int = 92
var grid_height: int = 42
var cell_count: int = 0

var target_mask: PackedByteArray = PackedByteArray()
var shadow_mask: PackedByteArray = PackedByteArray()
var target_count: int = 0
var target_ready: bool = false
var celebrated_up_to: int = -1

func _init():
	grid_width = maxi(8, int(round((ShadowCalculator.WALL_RIGHT - ShadowCalculator.WALL_LEFT) / CELL_SIZE)))
	grid_height = maxi(8, int(round((ShadowCalculator.WALL_BOTTOM - ShadowCalculator.WALL_TOP) / CELL_SIZE)))
	cell_count = grid_width * grid_height
	target_mask.resize(cell_count)
	shadow_mask.resize(cell_count)
	target_mask.fill(0)
	shadow_mask.fill(0)

func set_target(polygons: Array) -> void:
	target_mask.fill(0)
	target_count = 0
	for poly in polygons:
		paint_polygon(poly, target_mask)

	target_count = 0
	for i in range(cell_count):
		if target_mask[i] != 0:
			target_count += 1
	target_ready = target_count > 0
	celebrated_up_to = -1

func has_target() -> bool:
	return target_ready

func evaluate(shadow_polygons: Array) -> float:
	shadow_mask.fill(0)
	if not target_ready:
		return 0.0
	for poly in shadow_polygons:
		paint_polygon(poly, shadow_mask)
	return intersection_over_union()

func intersection_over_union() -> float:
	var inter: int = 0
	var union_cnt: int = 0
	for i in range(cell_count):
		var t: bool = target_mask[i] != 0
		var s: bool = shadow_mask[i] != 0
		if t and s: inter += 1
		if t or s: union_cnt += 1
	if union_cnt == 0:
		return 0.0
	return float(inter) / float(union_cnt)

func report_match(raw_iou: float) -> float:
	if MATCH_THRESHOLD <= 0.0: return 0.0
	var progress: float = raw_iou / MATCH_THRESHOLD
	return clampf(progress, 0.0, 1.0) * 100.0

func is_complete(raw_iou: float) -> bool:
	return target_ready and raw_iou >= MATCH_THRESHOLD

func paint_polygon(poly: PackedVector2Array, mask: PackedByteArray) -> bool:
	var n: int = poly.size()
	if n < 3:
		return false

	var cell_w: float = (ShadowCalculator.WALL_RIGHT - ShadowCalculator.WALL_LEFT) / float(grid_width)
	var cell_h: float = (ShadowCalculator.WALL_BOTTOM - ShadowCalculator.WALL_TOP) / float(grid_height)

	var min_y: float = INF
	var max_y: float = -INF
	for pt in poly:
		if pt.y < min_y: min_y = pt.y
		if pt.y > max_y: max_y = pt.y

	var row_start: int = row_for(min_y, cell_h)
	var row_end: int = row_for(max_y, cell_h)
	row_start = clampi(row_start, 0, grid_height - 1)
	row_end = clampi(row_end, 0, grid_height - 1)

	var painted: bool = false
	var xs: Array[float] = []

	for gy in range(row_start, row_end + 1):
		var y: float = ShadowCalculator.WALL_TOP + (float(gy) + 0.5) * cell_h
		xs.clear()

		for i in range(n):
			var j: int = (i + 1) % n
			var ay: float = poly[i].y
			var by: float = poly[j].y
			if (ay > y) == (by > y):
				continue
			var ax: float = poly[i].x
			var bx: float = poly[j].x
			var cross_x: float = ax + (y - ay) * (bx - ax) / (by - ay)
			xs.append(cross_x)

		if xs.size() < 2:
			continue
		xs.sort()

		var c: int = 0
		while c + 1 < xs.size():
			var gx0: int = col_for(xs[c], cell_w)
			var gx1: int = col_for(xs[c + 1], cell_w)
			gx0 = clampi(gx0, 0, grid_width - 1)
			gx1 = clampi(gx1, 0, grid_width - 1)

			for gx in range(gx0, gx1 + 1):
				var idx: int = gy * grid_width + gx
				if mask[idx] == 0:
					mask[idx] = 1
					painted = true
			c += 2

	return painted

func row_for(y: float, cell_h: float) -> int:
	return int(floor((y - ShadowCalculator.WALL_TOP) / cell_h))

func col_for(x: float, cell_w: float) -> int:
	return int(ceil((x - ShadowCalculator.WALL_LEFT) / cell_w - 0.5))

func newly_crossed_chimes(raw_iou: float) -> int:
	var fired: int = 0
	var pct: float = report_match(raw_iou)
	var marks: Array[float] = [50.0, 75.0, 100.0]
	for i in range(marks.size()):
		if pct >= marks[i] and celebrated_up_to < i:
			celebrated_up_to = i
			fired |= (1 << i)
	return fired

func reset_celebration() -> void:
	celebrated_up_to = -1
