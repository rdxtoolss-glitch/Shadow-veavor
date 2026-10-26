extends SceneTree

func _init():
	print("==================================================")
	print("GODOT 4 SHADOWWEAVER ARCHITECTURE & MATH VERIFICATION")
	print("==================================================")

	var matcher = SilhouetteMatcher.new()
	var total_levels = LevelLoader.count()
	print("Total levels registered: %d" % total_levels)
	assert(total_levels == 15, "Expected 15 levels")

	var failed = false

	# Test 1: Coordinate round-trip
	for lv_idx in range(total_levels):
		var sol_shapes = LevelLoader.create_solution_shapes(lv_idx)
		var lv = LevelLoader.get_level(lv_idx)
		for i in range(sol_shapes.size()):
			var s = sol_shapes[i]
			var auth = lv.solution[i]
			var proj_x = ShadowCalculator.wall_x_for(s.x, s.y)
			var proj_y = ShadowCalculator.wall_y_for(s.y)

			var diff_x = absf(proj_x - auth.wall_x)
			var diff_y = absf(proj_y - auth.wall_y)
			if diff_x > 0.05 or diff_y > 0.05:
				printerr("FAIL: LV%d shape %d roundtrip error: dx=%.4f, dy=%.4f" % [lv_idx + 1, i, diff_x, diff_y])
				failed = true

	# Test 2: IoU and solutions for all 15 levels
	for lv_idx in range(total_levels):
		var target_polys = LevelLoader.forge_target(lv_idx)
		matcher.set_target(target_polys)
		assert(matcher.has_target(), "Target must be ready")

		# Start IoU
		var start_shapes = LevelLoader.create_shapes(lv_idx)
		var start_polys: Array[PackedVector2Array] = []
		for s in start_shapes:
			start_polys.append(ShadowCalculator.project(s))
		var start_iou = matcher.evaluate(start_polys)

		# Solution IoU
		var sol_shapes = LevelLoader.create_solution_shapes(lv_idx)
		var sol_polys: Array[PackedVector2Array] = []
		for s in sol_shapes:
			sol_polys.append(ShadowCalculator.project(s))
		var sol_iou = matcher.evaluate(sol_polys)

		var lv_data = LevelLoader.get_level(lv_idx)
		print("LV%02d '%-10s' | Target cells: %4d | Start IoU: %5.1f%% | Solution IoU: %6.2f%%" % [
			lv_data.number,
			lv_data.title,
			matcher.target_count,
			start_iou * 100.0,
			sol_iou * 100.0
		])

		if sol_iou < 0.999:
			printerr("FAIL: LV%d solution IoU was %.4f, expected 1.0000" % [lv_idx + 1, sol_iou])
			failed = true

		if start_iou > 0.45:
			printerr("FAIL: LV%d starting layout is too close to solution (%.2f%%)" % [lv_idx + 1, start_iou * 100.0])
			failed = true

	if failed:
		printerr("VERIFICATION FAILED")
		quit(1)
	else:
		print("--------------------------------------------------")
		print("ALL 15 LEVELS VERIFIED 100% PERFECT IN GODOT 4!")
		print("--------------------------------------------------")
		quit(0)
