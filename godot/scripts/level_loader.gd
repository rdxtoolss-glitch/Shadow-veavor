class_name LevelLoader
extends RefCounted

static var _levels: Array[LevelData] = []
static var _initialized: bool = false

static func _sol(k: ShapeObject.Kind, w: float, h: float, wx: float, wy: float, rot: float) -> LevelData.SolutionShape:
	return LevelData.SolutionShape.new(k, w, h, wx, wy, rot)

static func _st(k: ShapeObject.Kind, w: float, h: float, sx: float, sy: float, rot: float) -> LevelData.StartShape:
	return LevelData.StartShape.new(k, w, h, sx, sy, rot)

static func _init_levels() -> void:
	if _initialized: return
	_initialized = true

	# LV 1: House
	var lv1 = LevelData.new(1, "House", "A body, a roof above it, a slim chimney beside it.",
		[
			_sol(ShapeObject.Kind.RECT, 280, 110, 460, 1420, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 300, 110, 460, 1300, 0),
			_sol(ShapeObject.Kind.LINE, 140, 28, 650, 1280, 90),
		],
		[
			_st(ShapeObject.Kind.RECT, 280, 110, 351.9, 716.9, 8),
			_st(ShapeObject.Kind.TRIANGLE, 300, 110, 728.4, 389.1, 267),
			_st(ShapeObject.Kind.LINE, 140, 28, 671.7, 788.1, 8),
		])

	# LV 2: Tree
	var lv2 = LevelData.new(2, "Tree", "Two canopies one above the other, a trunk beneath.",
		[
			_sol(ShapeObject.Kind.TRIANGLE, 260, 80, 500, 1230, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 300, 80, 500, 1350, 180),
			_sol(ShapeObject.Kind.RECT, 70, 120, 500, 1430, 0),
		],
		[
			_st(ShapeObject.Kind.TRIANGLE, 260, 80, 390.5, 627.3, 259),
			_st(ShapeObject.Kind.TRIANGLE, 300, 80, 647.7, 435.8, 241),
			_st(ShapeObject.Kind.RECT, 70, 120, 383.6, 302.9, 13),
		])

	# LV 3: Star
	var lv3 = LevelData.new(3, "Star", "Five rods, each turned a little further than the last.",
		[
			_sol(ShapeObject.Kind.LINE, 300, 28, 500, 1320, 18),
			_sol(ShapeObject.Kind.LINE, 300, 28, 500, 1320, 90),
			_sol(ShapeObject.Kind.LINE, 300, 28, 500, 1320, 162),
			_sol(ShapeObject.Kind.LINE, 300, 28, 500, 1320, 234),
			_sol(ShapeObject.Kind.LINE, 300, 28, 500, 1320, 306),
		],
		[
			_st(ShapeObject.Kind.LINE, 300, 28, 429.0, 537.8, 150),
			_st(ShapeObject.Kind.LINE, 300, 28, 551.6, 842.0, 66),
			_st(ShapeObject.Kind.LINE, 300, 28, 774.2, 567.2, 190),
			_st(ShapeObject.Kind.LINE, 300, 28, 199.6, 936.3, 314),
			_st(ShapeObject.Kind.LINE, 300, 28, 146.4, 322.8, 122),
		])

	# LV 4: Arrow
	var lv4 = LevelData.new(4, "Arrow", "A head, a long shaft, two bits of fletching.",
		[
			_sol(ShapeObject.Kind.TRIANGLE, 220, 120, 800, 1360, 90),
			_sol(ShapeObject.Kind.RECT, 260, 60, 480, 1380, 0),
			_sol(ShapeObject.Kind.LINE, 220, 26, 250, 1330, 0),
			_sol(ShapeObject.Kind.LINE, 160, 24, 300, 1250, 50),
		],
		[
			_st(ShapeObject.Kind.TRIANGLE, 220, 120, 467.6, 448.3, 41),
			_st(ShapeObject.Kind.RECT, 260, 60, 654.6, 1010.5, 33),
			_st(ShapeObject.Kind.LINE, 220, 26, 244.9, 971.2, 102),
			_st(ShapeObject.Kind.LINE, 160, 24, 709.0, 574.6, 106),
		])

	# LV 5: Fish
	var lv5 = LevelData.new(5, "Fish", "A round body, a tail behind it, fins above and below.",
		[
			_sol(ShapeObject.Kind.CIRCLE, 150, 150, 420, 1390, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 190, 130, 700, 1380, 90),
			_sol(ShapeObject.Kind.LINE, 190, 24, 330, 1250, -35),
			_sol(ShapeObject.Kind.LINE, 190, 24, 540, 1300, 30),
		],
		[
			_st(ShapeObject.Kind.CIRCLE, 150, 150, 506.2, 358.7, 292),
			_st(ShapeObject.Kind.TRIANGLE, 190, 130, 269.8, 562.6, 196),
			_st(ShapeObject.Kind.LINE, 190, 24, 567.9, 836.8, 212),
			_st(ShapeObject.Kind.LINE, 190, 24, 181.8, 874.4, 272),
		])

	# LV 6: Boat
	var lv6 = LevelData.new(6, "Boat", "A hull along the bottom, a sail and a mast above it.",
		[
			_sol(ShapeObject.Kind.RECT, 300, 90, 480, 1420, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 200, 130, 540, 1300, 0),
			_sol(ShapeObject.Kind.LINE, 150, 26, 400, 1300, 90),
			_sol(ShapeObject.Kind.TRIANGLE, 150, 110, 700, 1240, 0),
		],
		[
			_st(ShapeObject.Kind.RECT, 300, 90, 544.8, 1029.2, 182),
			_st(ShapeObject.Kind.TRIANGLE, 200, 130, 155.1, 796.0, 178),
			_st(ShapeObject.Kind.LINE, 150, 26, 295.8, 400.3, 14),
			_st(ShapeObject.Kind.TRIANGLE, 150, 110, 605.8, 628.2, 324),
		])

	# LV 7: Cat
	var lv7 = LevelData.new(7, "Cat", "A head with two ears, a body behind it, a curling tail.",
		[
			_sol(ShapeObject.Kind.CIRCLE, 130, 130, 350, 1300, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 110, 90, 290, 1220, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 110, 90, 420, 1220, 0),
			_sol(ShapeObject.Kind.CIRCLE, 160, 150, 620, 1400, 0),
			_sol(ShapeObject.Kind.LINE, 200, 26, 830, 1310, 40),
		],
		[
			_st(ShapeObject.Kind.CIRCLE, 130, 130, 583.4, 939.7, 73),
			_st(ShapeObject.Kind.TRIANGLE, 110, 90, 800.5, 1029.4, 160),
			_st(ShapeObject.Kind.TRIANGLE, 110, 90, 783.5, 799.9, 172),
			_st(ShapeObject.Kind.CIRCLE, 160, 150, 140.9, 435.1, 164),
			_st(ShapeObject.Kind.LINE, 200, 26, 545.3, 343.9, 157),
		])

	# LV 8: Bird
	var lv8 = LevelData.new(8, "Bird", "A body, a head, a beak, a tail and a raised wing.",
		[
			_sol(ShapeObject.Kind.CIRCLE, 150, 130, 470, 1400, 0),
			_sol(ShapeObject.Kind.CIRCLE, 110, 110, 280, 1250, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 110, 80, 150, 1230, 90),
			_sol(ShapeObject.Kind.TRIANGLE, 160, 110, 750, 1388, -40),
			_sol(ShapeObject.Kind.LINE, 180, 24, 600, 1290, -55),
		],
		[
			_st(ShapeObject.Kind.CIRCLE, 150, 130, 622.0, 850.1, 324),
			_st(ShapeObject.Kind.CIRCLE, 110, 110, 685.8, 502.7, 142),
			_st(ShapeObject.Kind.TRIANGLE, 110, 80, 870.1, 873.4, 185),
			_st(ShapeObject.Kind.TRIANGLE, 160, 110, 269.5, 986.6, 265),
			_st(ShapeObject.Kind.LINE, 180, 24, 241.4, 400.7, 180),
		])

	# LV 9: Butterfly
	var lv9 = LevelData.new(9, "Butterfly", "A body with two wings either side, high and low.",
		[
			_sol(ShapeObject.Kind.RECT, 60, 220, 500, 1360, 90),
			_sol(ShapeObject.Kind.CIRCLE, 130, 130, 350, 1280, 0),
			_sol(ShapeObject.Kind.CIRCLE, 130, 130, 350, 1420, 0),
			_sol(ShapeObject.Kind.CIRCLE, 130, 130, 650, 1280, 0),
			_sol(ShapeObject.Kind.CIRCLE, 130, 130, 650, 1420, 0),
		],
		[
			_st(ShapeObject.Kind.RECT, 60, 220, 660.6, 760.6, 215),
			_st(ShapeObject.Kind.CIRCLE, 130, 130, 382.3, 634.1, 282),
			_st(ShapeObject.Kind.CIRCLE, 130, 130, 238.8, 839.1, 127),
			_st(ShapeObject.Kind.CIRCLE, 130, 130, 460.7, 387.7, 300),
			_st(ShapeObject.Kind.CIRCLE, 130, 130, 433.2, 1042.9, 14),
		])

	# LV 10: Flower
	var lv10 = LevelData.new(10, "Flower", "Six petals, evenly spaced, around a centre.",
		[
			_sol(ShapeObject.Kind.CIRCLE, 120, 120, 500, 1350, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 150, 110, 500, 1230, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 150, 110, 660, 1290, 60),
			_sol(ShapeObject.Kind.TRIANGLE, 150, 110, 610, 1425, 120),
			_sol(ShapeObject.Kind.TRIANGLE, 150, 110, 390, 1425, 240),
			_sol(ShapeObject.Kind.TRIANGLE, 150, 110, 340, 1290, 300),
		],
		[
			_st(ShapeObject.Kind.CIRCLE, 120, 120, 699.1, 671.1, 106),
			_st(ShapeObject.Kind.TRIANGLE, 150, 110, 456.5, 969.5, 106),
			_st(ShapeObject.Kind.TRIANGLE, 150, 110, 818.2, 897.8, 287),
			_st(ShapeObject.Kind.TRIANGLE, 150, 110, 240.6, 364.0, 187),
			_st(ShapeObject.Kind.TRIANGLE, 150, 110, 199.6, 749.1, 325),
			_st(ShapeObject.Kind.TRIANGLE, 150, 110, 557.6, 316.0, 57),
		])

	# LV 11: Candle
	var lv11 = LevelData.new(11, "Candle", "A body, a wick, a flame on top.",
		[
			_sol(ShapeObject.Kind.RECT, 160, 130, 500, 1420, 0),
			_sol(ShapeObject.Kind.LINE, 110, 24, 500, 1310, 90),
			_sol(ShapeObject.Kind.TRIANGLE, 130, 100, 500, 1225, 0),
		],
		[
			_st(ShapeObject.Kind.RECT, 160, 130, 737.7, 581.5, 357),
			_st(ShapeObject.Kind.LINE, 110, 24, 341.8, 442.9, 88),
			_st(ShapeObject.Kind.TRIANGLE, 130, 100, 412.2, 909.9, 158),
		])

	# LV 12: Key
	var lv12 = LevelData.new(12, "Key", "A round head, a straight shaft, two teeth hanging off it.",
		[
			_sol(ShapeObject.Kind.CIRCLE, 140, 140, 250, 1350, 0),
			_sol(ShapeObject.Kind.RECT, 240, 50, 540, 1355, 0),
			_sol(ShapeObject.Kind.LINE, 90, 24, 700, 1430, 90),
			_sol(ShapeObject.Kind.LINE, 90, 24, 820, 1430, 90),
		],
		[
			_st(ShapeObject.Kind.CIRCLE, 140, 140, 776.3, 492.0, 248),
			_st(ShapeObject.Kind.RECT, 240, 50, 227.2, 676.3, 70),
			_st(ShapeObject.Kind.LINE, 90, 24, 766.3, 922.1, 29),
			_st(ShapeObject.Kind.LINE, 90, 24, 560.4, 316.6, 323),
		])

	# LV 13: Person
	var lv13 = LevelData.new(13, "Person", "A head, a body, arms out, two legs below.",
		[
			_sol(ShapeObject.Kind.CIRCLE, 110, 110, 500, 1225, 0),
			_sol(ShapeObject.Kind.RECT, 150, 150, 500, 1350, 0),
			_sol(ShapeObject.Kind.LINE, 170, 24, 300, 1340, 65),
			_sol(ShapeObject.Kind.LINE, 170, 24, 700, 1340, -65),
			_sol(ShapeObject.Kind.LINE, 130, 24, 410, 1425, 80),
			_sol(ShapeObject.Kind.LINE, 130, 24, 590, 1425, -80),
		],
		[
			_st(ShapeObject.Kind.CIRCLE, 110, 110, 814.9, 402.5, 138),
			_st(ShapeObject.Kind.RECT, 150, 150, 872.5, 909.7, 52),
			_st(ShapeObject.Kind.LINE, 170, 24, 360.3, 934.3, 260),
			_st(ShapeObject.Kind.LINE, 170, 24, 340.3, 292.9, 211),
			_st(ShapeObject.Kind.LINE, 130, 24, 162.0, 460.6, 105),
			_st(ShapeObject.Kind.LINE, 130, 24, 559.2, 477.6, 29),
		])

	# LV 14: Peaks
	var lv14 = LevelData.new(14, "Peaks", "Three mountains of different sizes over a shared base.",
		[
			_sol(ShapeObject.Kind.TRIANGLE, 200, 100, 260, 1320, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 220, 100, 530, 1250, 0),
			_sol(ShapeObject.Kind.TRIANGLE, 170, 90, 790, 1400, 0),
			_sol(ShapeObject.Kind.RECT, 400, 70, 520, 1425, 0),
		],
		[
			_st(ShapeObject.Kind.TRIANGLE, 200, 100, 853.5, 312.9, 29),
			_st(ShapeObject.Kind.TRIANGLE, 220, 100, 192.8, 884.4, 310),
			_st(ShapeObject.Kind.TRIANGLE, 170, 90, 397.2, 556.9, 196),
			_st(ShapeObject.Kind.RECT, 400, 70, 567.1, 994.5, 160),
		])

	# LV 15: Compass
	var lv15 = LevelData.new(15, "Compass", "Four long points, four short ones, and a hub at the centre.",
		[
			_sol(ShapeObject.Kind.LINE, 300, 26, 500, 1320, 0),
			_sol(ShapeObject.Kind.LINE, 300, 26, 500, 1320, 90),
			_sol(ShapeObject.Kind.LINE, 205, 24, 500, 1320, 45),
			_sol(ShapeObject.Kind.LINE, 205, 24, 500, 1320, 135),
			_sol(ShapeObject.Kind.CIRCLE, 135, 135, 500, 1320, 0),
			_sol(ShapeObject.Kind.SQUARE, 100, 100, 500, 1320, 0),
		],
		[
			_st(ShapeObject.Kind.LINE, 300, 26, 132.1, 983.4, 280),
			_st(ShapeObject.Kind.LINE, 300, 26, 643.2, 616.4, 16),
			_st(ShapeObject.Kind.LINE, 205, 24, 867.1, 830.1, 338),
			_st(ShapeObject.Kind.LINE, 205, 24, 437.0, 819.1, 292),
			_st(ShapeObject.Kind.CIRCLE, 135, 135, 290.2, 313.4, 83),
			_st(ShapeObject.Kind.SQUARE, 100, 100, 275.0, 526.4, 343),
		])

	_levels = [lv1, lv2, lv3, lv4, lv5, lv6, lv7, lv8, lv9, lv10, lv11, lv12, lv13, lv14, lv15]

static func count() -> int:
	_init_levels()
	return _levels.size()

static func get_level(index: int) -> LevelData:
	_init_levels()
	var idx = clampi(index, 0, _levels.size() - 1)
	return _levels[idx]

static func create_shapes(index: int) -> Array[ShapeObject]:
	return get_level(index).create_starting_shapes()

static func create_solution_shapes(index: int) -> Array[ShapeObject]:
	var lv = get_level(index)
	var out: Array[ShapeObject] = []
	for s in lv.solution:
		var pos: Vector2 = ShadowCalculator.solve_stage_position_for(s.wall_x, s.wall_y)
		out.append(ShapeObject.new(s.kind, s.width, s.height, pos.x, pos.y, s.rotation))
	return out

static func forge_target(index: int) -> Array[PackedVector2Array]:
	var sol = create_solution_shapes(index)
	var polys: Array[PackedVector2Array] = []
	for shape in sol:
		polys.append(ShadowCalculator.project(shape))
	return polys
