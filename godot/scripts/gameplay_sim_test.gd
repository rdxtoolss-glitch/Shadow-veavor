extends SceneTree

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
	print("Testing live playthrough simulation for %d levels..." % total_levels)

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
			printerr("FAIL: Level %d did not complete with solution layout!" % (lv_idx + 1))
			quit(1)
			return

		print("  ✔ Level %02d '%-10s' solved and completed successfully!" % [lv_data.number, lv_data.title])

	print("--------------------------------------------------")
	print("ALL 15 LEVELS SOLVED & SIMULATED SUCCESSFULLY IN GODOT 4!")
	print("--------------------------------------------------")
	quit(0)
