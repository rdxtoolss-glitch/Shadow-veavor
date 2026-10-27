class_name LevelData
extends RefCounted

class SolutionShape:
	var kind: ShapeObject.Kind
	var width: float
	var height: float
	var wall_x: float
	var wall_y: float
	var rotation: float

	func _init(p_k: ShapeObject.Kind, p_w: float, p_h: float, p_wx: float, p_wy: float, p_rot: float):
		kind = p_k
		width = p_w
		height = p_h
		wall_x = p_wx
		wall_y = p_wy
		rotation = p_rot

class StartShape:
	var kind: ShapeObject.Kind
	var width: float
	var height: float
	var stage_x: float
	var stage_y: float
	var rotation: float

	func _init(p_k: ShapeObject.Kind, p_w: float, p_h: float, p_sx: float, p_sy: float, p_rot: float):
		kind = p_k
		width = p_w
		height = p_h
		stage_x = p_sx
		stage_y = p_sy
		rotation = p_rot

var number: int = 1
var chapter: int = 1
var chapter_title: String = ""
var title: String = ""
var hint: String = ""
var par_seconds: float = 30.0
var solution: Array[SolutionShape] = []
var start: Array[StartShape] = []

func _init(p_num: int, p_title: String, p_hint: String, p_sol: Array[SolutionShape], p_start: Array[StartShape], p_chap: int = 1, p_ch_title: String = "", p_par: float = 30.0):
	number = p_num
	title = p_title
	hint = p_hint
	solution = p_sol
	start = p_start
	chapter = p_chap
	chapter_title = p_ch_title
	par_seconds = p_par

func shape_count() -> int:
	return solution.size()

func create_starting_shapes() -> Array[ShapeObject]:
	var out: Array[ShapeObject] = []
	for i in range(start.size()):
		var st: StartShape = start[i]
		var s: ShapeObject = ShapeObject.new(st.kind, st.width, st.height, st.stage_x, st.stage_y, st.rotation)
		apply_idle_drift(s, i)
		out.append(s)
	return out

static func apply_idle_drift(s: ShapeObject, i: int) -> void:
	var a: float = float(posmod(i * 2654435761, 1000)) / 1000.0
	var b: float = float(posmod(i * 40503 + 17, 1000)) / 1000.0
	var c: float = float(posmod(i * 2246822519, 1000)) / 1000.0
	s.vx = (a - 0.5) * 7.0
	s.vy = (b - 0.5) * 5.0
	s.spin = (c - 0.5) * 3.5
