extends SceneTree

func _init():
	print("==================================================")
	print("GODOT 4: 100-MISSION CAMPAIGN VERIFICATION SUITE")
	print("==================================================")

	var total_levels = LevelLoader.count()
	print("Total campaign missions loaded: %d" % total_levels)
	if total_levels != 100:
		printerr("FAIL: Expected 100 levels, got %d" % total_levels)
		quit(1)
		return

	var matcher = SilhouetteMatcher.new()
	var failed = false

	# Test all 100 missions
	for lv_idx in range(total_levels):
		var lv = LevelLoader.get_level(lv_idx)
		var target_polys = LevelLoader.forge_target(lv_idx)
		matcher.set_target(target_polys)

		# Solution IoU
		var sol_shapes = LevelLoader.create_solution_shapes(lv_idx)
		var sol_polys: Array[PackedVector2Array] = []
		for s in sol_shapes:
			sol_polys.append(ShadowCalculator.project(s))
		var sol_iou = matcher.evaluate(sol_polys)

		# Start IoU
		var start_shapes = LevelLoader.create_shapes(lv_idx)
		var start_polys: Array[PackedVector2Array] = []
		for s in start_shapes:
			start_polys.append(ShadowCalculator.project(s))
		var start_iou = matcher.evaluate(start_polys)

		if lv_idx % 10 == 0 or sol_iou < 0.999 or start_iou > 0.45:
			print("M%03d [Ch.%02d] '%-20s' | Cells: %4d | Start: %5.1f%% | Sol: %6.2f%%" % [
				lv.number,
				lv.chapter,
				lv.title,
				matcher.target_count,
				start_iou * 100.0,
				sol_iou * 100.0
			])

		if sol_iou < 0.999:
			printerr("FAIL: Mission %d solution IoU was %.4f, expected 1.0000" % [lv.number, sol_iou])
			failed = true

		if start_iou > 0.45:
			printerr("FAIL: Mission %d starting layout too close to solution (%.2f%%)" % [lv.number, start_iou * 100.0])
			failed = true

	if failed:
		printerr("100-MISSION VERIFICATION FAILED")
		quit(1)
	else:
		print("--------------------------------------------------")
		print("ALL 100 CAMPAIGN MISSIONS VERIFIED 100% PERFECT IN GODOT 4!")
		print("--------------------------------------------------")
		quit(0)
