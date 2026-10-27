extends SceneTree

const SaveManager = preload("res://scripts/save_manager.gd")
const ChapterSelectModal = preload("res://scripts/chapter_select_modal.gd")

func _init():
	process_frame.connect(_run_test, CONNECT_ONE_SHOT)

func _run_test():
	print("==================================================")
	print("GODOT 4 GAMEPLAY & SIMULATION INTEGRATION TEST")
	print("==================================================")

	var main_scene = load("res://scenes/main.tscn")
	assert(main_scene != null, "main.tscn must load")
	var main_node = main_scene.instantiate() as Main
	root.add_child(main_node)

	# Wait a frame for @onready bindings and _ready()
	await process_frame

	var total_levels = LevelLoader.count()
	print("Testing live playthrough simulation for all %d campaign missions..." % total_levels)

	# 1. Test Codex / Chapter Select Modal opening and chapter navigation
	assert(main_node.chapter_select_modal != null, "ChapterSelectModal must be present")
	main_node.hud.codex_pressed.emit()
	assert(main_node.chapter_select_modal.visible, "ChapterSelectModal should open on codex_pressed")
	main_node.chapter_select_modal.select_chapter(3)
	assert(main_node.chapter_select_modal.current_chapter == 3, "Chapter selection must update")
	main_node.chapter_select_modal.close()

	# 2. Test solving all 100 missions
	for lv_idx in range(total_levels):
		main_node.load_level(lv_idx)
		var lv_data = LevelLoader.get_level(lv_idx)

		# Starting state check
		assert(not main_node.level_is_complete, "Level should not start completed")
		assert(main_node.live_tokens.size() == lv_data.shape_count(), "Shape count mismatch")

		# Solve level by snapping shapes to solution
		var sol_shapes = LevelLoader.create_solution_shapes(lv_idx)
		for i in range(main_node.live_tokens.size()):
			var token = main_node.live_tokens[i]
			var sol = sol_shapes[i]
			token.shape_data.x = sol.x
			token.shape_data.y = sol.y
			token.shape_data.rotation = sol.rotation

		# Re-evaluate
		main_node._evaluate_shadows()

		if not main_node.level_is_complete:
			printerr("FAIL: Mission %d did not complete with solution layout!" % (lv_idx + 1))
			quit(1)
			return

		if (lv_idx + 1) % 10 == 0 or lv_idx == 0:
			print("  ✔ Mission %03d [Ch %02d] '%-18s' solved & completed (Stars: %d)!" % [
				lv_data.number,
				lv_data.chapter,
				lv_data.title,
				SaveManager.get_level_stars(lv_data.number)
			])

	print("--------------------------------------------------")
	print("ALL %d CAMPAIGN MISSIONS SOLVED & SIMULATED 100%% IN GODOT 4!" % total_levels)
	print("Total Stars Earned: %d / 300" % SaveManager.get_total_stars())
	print("--------------------------------------------------")
	quit(0)
