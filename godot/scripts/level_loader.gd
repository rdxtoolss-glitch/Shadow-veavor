class_name LevelLoader
extends RefCounted

static var _levels: Array[LevelData] = []
static var _initialized: bool = false

static func _init_levels() -> void:
	if _initialized: return
	_initialized = true

	var file = FileAccess.open("res://data/missions.json", FileAccess.READ)
	if not file:
		printerr("ERROR: Could not open res://data/missions.json!")
		return

	var json_str = file.get_as_text()
	file.close()

	var json = JSON.new()
	var err = json.parse(json_str)
	if err != OK:
		printerr("ERROR: JSON parse failed on missions.json: ", json.get_error_message())
		return

	var raw_list = json.data as Array
	if not raw_list:
		printerr("ERROR: missions.json root is not an array!")
		return

	_levels.clear()
	for m_dict in raw_list:
		var sol_shapes: Array[LevelData.SolutionShape] = []
		for s in m_dict["solution"]:
			sol_shapes.append(LevelData.SolutionShape.new(
				int(s["kind"]) as ShapeObject.Kind,
				float(s["width"]),
				float(s["height"]),
				float(s["wall_x"]),
				float(s["wall_y"]),
				float(s["rotation"])
			))

		var start_shapes: Array[LevelData.StartShape] = []
		for st in m_dict["start"]:
			start_shapes.append(LevelData.StartShape.new(
				int(st["kind"]) as ShapeObject.Kind,
				float(st["width"]),
				float(st["height"]),
				float(st["stage_x"]),
				float(st["stage_y"]),
				float(st["rotation"])
			))

		var num: int = int(m_dict.get("number", _levels.size() + 1))
		var title: String = str(m_dict.get("title", "Mission %d" % num))
		var hint: String = str(m_dict.get("hint", ""))
		var ch_num: int = int(m_dict.get("chapter", (num - 1) / 10 + 1))
		var ch_title: String = str(m_dict.get("chapter_title", "Chapter %d" % ch_num))
		var par: float = float(m_dict.get("par_seconds", 30.0))

		var lv = LevelData.new(num, title, hint, sol_shapes, start_shapes, ch_num, ch_title, par)
		_levels.append(lv)

static func count() -> int:
	_init_levels()
	return _levels.size()

static func get_level(index: int) -> LevelData:
	_init_levels()
	var idx = clampi(index, 0, _levels.size() - 1)
	return _levels[idx]

static func get_chapter_levels(chapter_num: int) -> Array[LevelData]:
	_init_levels()
	var out: Array[LevelData] = []
	for lv in _levels:
		if lv.chapter == chapter_num:
			out.append(lv)
	return out

static func chapter_count() -> int:
	return 10

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
