class_name SaveManager
extends RefCounted

const SAVE_PATH: String = "user://shadow_save.json"

static var unlocked_max: int = 1
static var stars: Dictionary = {} # String(level_num) -> int
static var best_times: Dictionary = {} # String(level_num) -> float
static var sound_enabled: bool = true
static var haptics_enabled: bool = true
static var debug_unlock_all: bool = false
static var _is_initialized: bool = false

static func init_if_needed():
	if _is_initialized:
		return
	_is_initialized = true
	load_data()

static func load_data():
	if not FileAccess.file_exists(SAVE_PATH):
		unlocked_max = 1
		stars = {}
		best_times = {}
		sound_enabled = true
		haptics_enabled = true
		return

	var file = FileAccess.open(SAVE_PATH, FileAccess.READ)
	if not file:
		return

	var json_str = file.get_as_text()
	file.close()

	var test_json = JSON.new()
	var err = test_json.parse(json_str)
	if err != OK or not (test_json.data is Dictionary):
		return

	var d: Dictionary = test_json.data
	unlocked_max = int(d.get("unlocked_max", 1))
	unlocked_max = clampi(unlocked_max, 1, 100)
	stars = d.get("stars", {})
	best_times = d.get("best_times", {})
	sound_enabled = bool(d.get("sound_enabled", true))
	haptics_enabled = bool(d.get("haptics_enabled", true))
	debug_unlock_all = bool(d.get("debug_unlock_all", false))

static func save_data():
	var d: Dictionary = {
		"unlocked_max": unlocked_max,
		"stars": stars,
		"best_times": best_times,
		"sound_enabled": sound_enabled,
		"haptics_enabled": haptics_enabled,
		"debug_unlock_all": debug_unlock_all
	}
	var file = FileAccess.open(SAVE_PATH, FileAccess.WRITE)
	if file:
		file.store_string(JSON.stringify(d, "\t"))
		file.close()

static func is_level_unlocked(level_num: int) -> bool:
	init_if_needed()
	if debug_unlock_all:
		return true
	return level_num <= unlocked_max

static func get_level_stars(level_num: int) -> int:
	init_if_needed()
	var key = str(level_num)
	return int(stars.get(key, 0))

static func get_level_best_time(level_num: int) -> float:
	init_if_needed()
	var key = str(level_num)
	return float(best_times.get(key, 0.0))

static func is_level_completed(level_num: int) -> bool:
	return get_level_stars(level_num) > 0

static func record_completion(level_num: int, time_sec: float, earned_stars: int):
	init_if_needed()
	var key = str(level_num)
	var prev_stars = int(stars.get(key, 0))
	if earned_stars > prev_stars:
		stars[key] = earned_stars

	var prev_time = float(best_times.get(key, 999999.0))
	if time_sec < prev_time or prev_time <= 0.0:
		best_times[key] = time_sec

	if level_num >= unlocked_max and level_num < 100:
		unlocked_max = level_num + 1

	save_data()

static func get_total_stars() -> int:
	init_if_needed()
	var total: int = 0
	for k in stars:
		total += int(stars[k])
	return total

static func get_chapter_stars(chapter_num: int) -> int:
	init_if_needed()
	var start_lvl = (chapter_num - 1) * 10 + 1
	var end_lvl = chapter_num * 10
	var total: int = 0
	for lvl in range(start_lvl, end_lvl + 1):
		total += get_level_stars(lvl)
	return total

static func get_chapter_completed_count(chapter_num: int) -> int:
	init_if_needed()
	var start_lvl = (chapter_num - 1) * 10 + 1
	var end_lvl = chapter_num * 10
	var count: int = 0
	for lvl in range(start_lvl, end_lvl + 1):
		if is_level_completed(lvl):
			count += 1
	return count

static func toggle_unlock_all() -> bool:
	init_if_needed()
	debug_unlock_all = not debug_unlock_all
	save_data()
	return debug_unlock_all
