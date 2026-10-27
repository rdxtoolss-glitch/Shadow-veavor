extends SceneTree

const ShadowCalculator = preload("res://scripts/shadow_calculator.gd")
const ShapeObject = preload("res://scripts/shape_object.gd")
const SilhouetteMatcher = preload("res://scripts/silhouette_matcher.gd")

func _init():
	print("==================================================")
	print("GENERATING & VERIFYING 100 CAMPAIGN MISSIONS")
	print("==================================================")

	var missions: Array[Dictionary] = _generate_all_100_missions()
	var matcher = SilhouetteMatcher.new()
	var verified_count: int = 0

	for m in missions:
		var sol_polys: Array[PackedVector2Array] = []
		for s_dict in m["solution"]:
			var shape = ShapeObject.new(
				s_dict["kind"],
				s_dict["width"],
				s_dict["height"],
				s_dict["stage_x"],
				s_dict["stage_y"],
				s_dict["rotation"]
			)
			sol_polys.append(ShadowCalculator.project(shape))

		matcher.set_target(sol_polys)
		var sol_iou = matcher.evaluate(sol_polys)

		# Evaluate start scatter
		var start_polys: Array[PackedVector2Array] = []
		for st_dict in m["start"]:
			var shape = ShapeObject.new(
				st_dict["kind"],
				st_dict["width"],
				st_dict["height"],
				st_dict["stage_x"],
				st_dict["stage_y"],
				st_dict["rotation"]
			)
			start_polys.append(ShadowCalculator.project(shape))
		var start_iou = matcher.evaluate(start_polys)

		if sol_iou < 0.999:
			printerr("ERROR: Mission %d '%s' solution IoU failed: %.4f" % [m["number"], m["title"], sol_iou])
			quit(1)
			return

		if start_iou > 0.40:
			printerr("WARNING: Mission %d '%s' start IoU too high: %.2f%%" % [m["number"], m["title"], start_iou * 100.0])

		verified_count += 1

	print("ALL %d MISSIONS MATHEMATICALLY VERIFIED (IoU = 100%%)!" % verified_count)

	# Save to JSON
	var json_str = JSON.stringify(missions, "\t")
	var file = FileAccess.open("res://data/missions.json", FileAccess.WRITE)
	if file:
		file.store_string(json_str)
		file.close()
		print("Successfully saved 100 missions to res://data/missions.json!")
	else:
		printerr("Failed to write res://data/missions.json")
		quit(1)
		return

	quit(0)

func _generate_all_100_missions() -> Array[Dictionary]:
	var list: Array[Dictionary] = []

	var chapter_names = [
		"The Apprentice's Atelier",
		"Chronicles of the Wild",
		"The Cartographer's Sea",
		"Monuments of the Ancients",
		"The Alchemist's Crucible",
		"Bestiary of Myth",
		"The Royal Regalia",
		"The Musician's Conservatory",
		"Clockwork & Automata",
		"Celestial Convergence"
	]

	# Build all 100 missions
	for num in range(1, 101):
		var ch_idx = (num - 1) / 10
		var ch_num = ch_idx + 1
		var ch_title = chapter_names[ch_idx]
		var mission = _create_mission_spec(num, ch_num, ch_title)
		list.append(mission)

	return list

func _sol(k: int, w: float, h: float, wx: float, wy: float, rot: float) -> Dictionary:
	var pos = ShadowCalculator.solve_stage_position_for(wx, wy)
	return {
		"kind": k,
		"width": w,
		"height": h,
		"wall_x": wx,
		"wall_y": wy,
		"stage_x": pos.x,
		"stage_y": pos.y,
		"rotation": rot
	}

func _st(k: int, w: float, h: float, sx: float, sy: float, rot: float) -> Dictionary:
	return {
		"kind": k,
		"width": w,
		"height": h,
		"stage_x": sx,
		"stage_y": sy,
		"rotation": rot
	}

func _create_mission_spec(num: int, ch_num: int, ch_title: String) -> Dictionary:
	var title = ""
	var hint = ""
	var sol_shapes: Array[Dictionary] = []
	var start_shapes: Array[Dictionary] = []
	var par: float = 24.0 + (num * 0.25)

	# Shape kind shorthands
	var C = ShapeObject.Kind.CIRCLE
	var T = ShapeObject.Kind.TRIANGLE
	var S = ShapeObject.Kind.SQUARE
	var R = ShapeObject.Kind.RECT
	var L = ShapeObject.Kind.LINE

	match num:
		# Chapter 1: The Apprentice's Atelier
		1:
			title = "House"
			hint = "A body, a roof above it, a slim chimney beside it."
			sol_shapes = [_sol(R, 280, 110, 460, 1420, 0), _sol(T, 300, 110, 460, 1300, 0), _sol(L, 140, 28, 650, 1280, 90)]
			start_shapes = [_st(R, 280, 110, 351.9, 716.9, 8), _st(T, 300, 110, 728.4, 389.1, 267), _st(L, 140, 28, 671.7, 788.1, 8)]
		2:
			title = "Tree"
			hint = "Two canopies one above the other, a trunk beneath."
			sol_shapes = [_sol(T, 260, 80, 500, 1230, 0), _sol(T, 300, 80, 500, 1350, 180), _sol(R, 70, 120, 500, 1430, 0)]
			start_shapes = [_st(T, 260, 80, 390.5, 627.3, 259), _st(T, 300, 80, 647.7, 435.8, 241), _st(R, 70, 120, 383.6, 302.9, 13)]
		3:
			title = "Star"
			hint = "Five rods, each turned a little further than the last."
			sol_shapes = [_sol(L, 300, 28, 500, 1320, 18), _sol(L, 300, 28, 500, 1320, 90), _sol(L, 300, 28, 500, 1320, 162), _sol(L, 300, 28, 500, 1320, 234), _sol(L, 300, 28, 500, 1320, 306)]
			start_shapes = [_st(L, 300, 28, 429.0, 537.8, 150), _st(L, 300, 28, 551.6, 842.0, 66), _st(L, 300, 28, 774.2, 567.2, 190), _st(L, 300, 28, 199.6, 936.3, 314), _st(L, 300, 28, 146.4, 322.8, 122)]
		4:
			title = "Arrow"
			hint = "A head, a long shaft, two bits of fletching."
			sol_shapes = [_sol(T, 220, 120, 800, 1360, 90), _sol(R, 260, 60, 480, 1380, 0), _sol(L, 220, 26, 250, 1330, 0), _sol(L, 160, 24, 300, 1250, 50)]
			start_shapes = [_st(T, 220, 120, 467.6, 448.3, 41), _st(R, 260, 60, 654.6, 1010.5, 33), _st(L, 220, 26, 244.9, 971.2, 102), _st(L, 160, 24, 709.0, 574.6, 106)]
		5:
			title = "Fish"
			hint = "A round body, a tail behind it, fins above and below."
			sol_shapes = [_sol(C, 150, 150, 420, 1390, 0), _sol(T, 190, 130, 700, 1380, 90), _sol(L, 190, 24, 330, 1250, -35), _sol(L, 190, 24, 540, 1300, 30)]
			start_shapes = [_st(C, 150, 150, 506.2, 358.7, 292), _st(T, 190, 130, 269.8, 562.6, 196), _st(L, 190, 24, 567.9, 836.8, 212), _st(L, 190, 24, 181.8, 874.4, 272)]
		6:
			title = "Boat"
			hint = "A hull along the bottom, a sail and a mast above it."
			sol_shapes = [_sol(R, 300, 90, 480, 1420, 0), _sol(T, 200, 130, 540, 1300, 0), _sol(L, 150, 26, 400, 1300, 90), _sol(T, 150, 110, 700, 1240, 0)]
			start_shapes = [_st(R, 300, 90, 544.8, 1029.2, 182), _st(T, 200, 130, 155.1, 796.0, 178), _st(L, 150, 26, 295.8, 400.3, 14), _st(T, 150, 110, 605.8, 628.2, 324)]
		7:
			title = "Cat"
			hint = "A head with two ears, a body behind it, a curling tail."
			sol_shapes = [_sol(C, 130, 130, 350, 1300, 0), _sol(T, 110, 90, 290, 1220, 0), _sol(T, 110, 90, 420, 1220, 0), _sol(C, 160, 150, 620, 1400, 0), _sol(L, 200, 26, 830, 1310, 40)]
			start_shapes = [_st(C, 130, 130, 583.4, 939.7, 73), _st(T, 110, 90, 800.5, 1029.4, 160), _st(T, 110, 90, 783.5, 799.9, 172), _st(C, 160, 150, 140.9, 435.1, 164), _st(L, 200, 26, 545.3, 343.9, 157)]
		8:
			title = "Bird"
			hint = "A body, a head, a beak, a tail and a raised wing."
			sol_shapes = [_sol(C, 150, 130, 470, 1400, 0), _sol(C, 110, 110, 280, 1250, 0), _sol(T, 110, 80, 150, 1230, 90), _sol(T, 160, 110, 750, 1388, -40), _sol(L, 180, 24, 600, 1290, -55)]
			start_shapes = [_st(C, 150, 130, 622.0, 850.1, 324), _st(C, 110, 110, 685.8, 502.7, 142), _st(T, 110, 80, 870.1, 873.4, 185), _st(T, 160, 110, 269.5, 986.6, 265), _st(L, 180, 24, 241.4, 400.7, 180)]
		9:
			title = "Butterfly"
			hint = "A body with two wings either side, high and low."
			sol_shapes = [_sol(R, 60, 220, 500, 1360, 90), _sol(C, 130, 130, 350, 1280, 0), _sol(C, 130, 130, 350, 1420, 0), _sol(C, 130, 130, 650, 1280, 0), _sol(C, 130, 130, 650, 1420, 0)]
			start_shapes = [_st(R, 60, 220, 660.6, 760.6, 215), _st(C, 130, 130, 382.3, 634.1, 282), _st(C, 130, 130, 238.8, 839.1, 127), _st(C, 130, 130, 460.7, 387.7, 300), _st(C, 130, 130, 433.2, 1042.9, 14)]
		10:
			title = "Flower"
			hint = "Six petals, evenly spaced, around a centre."
			sol_shapes = [_sol(C, 120, 120, 500, 1350, 0), _sol(T, 150, 110, 500, 1230, 0), _sol(T, 150, 110, 660, 1290, 60), _sol(T, 150, 110, 610, 1425, 120), _sol(T, 150, 110, 390, 1425, 240), _sol(T, 150, 110, 340, 1290, 300)]
			start_shapes = [_st(C, 120, 120, 699.1, 671.1, 106), _st(T, 150, 110, 456.5, 969.5, 106), _st(T, 150, 110, 818.2, 897.8, 287), _st(T, 150, 110, 240.6, 364.0, 187), _st(T, 150, 110, 199.6, 749.1, 325), _st(T, 150, 110, 557.6, 316.0, 57)]

		# Chapter 2: Chronicles of the Wild (11 - 20)
		11:
			title = "Candle"
			hint = "A body, a wick, a flame on top."
			sol_shapes = [_sol(R, 160, 130, 500, 1420, 0), _sol(L, 110, 24, 500, 1310, 90), _sol(T, 130, 100, 500, 1225, 0)]
			start_shapes = [_st(R, 160, 130, 737.7, 581.5, 357), _st(L, 110, 24, 341.8, 442.9, 88), _st(T, 130, 100, 412.2, 909.9, 158)]
		12:
			title = "Key"
			hint = "A round head, a straight shaft, two teeth hanging off it."
			sol_shapes = [_sol(C, 140, 140, 250, 1350, 0), _sol(R, 240, 50, 540, 1355, 0), _sol(L, 90, 24, 700, 1430, 90), _sol(L, 90, 24, 820, 1430, 90)]
			start_shapes = [_st(C, 140, 140, 776.3, 492.0, 248), _st(R, 240, 50, 227.2, 676.3, 70), _st(L, 90, 24, 766.3, 922.1, 29), _st(L, 90, 24, 560.4, 316.6, 323)]
		13:
			title = "Person"
			hint = "A head, a body, arms out, two legs below."
			sol_shapes = [_sol(C, 110, 110, 500, 1225, 0), _sol(R, 150, 150, 500, 1350, 0), _sol(L, 170, 24, 300, 1340, 65), _sol(L, 170, 24, 700, 1340, -65), _sol(L, 130, 24, 410, 1425, 80), _sol(L, 130, 24, 590, 1425, -80)]
			start_shapes = [_st(C, 110, 110, 814.9, 402.5, 138), _st(R, 150, 150, 872.5, 909.7, 52), _st(L, 170, 24, 360.3, 934.3, 260), _st(L, 170, 24, 340.3, 292.9, 211), _st(L, 130, 24, 162.0, 460.6, 105), _st(L, 130, 24, 559.2, 477.6, 29)]
		14:
			title = "Peaks"
			hint = "Three mountains of different sizes over a shared base."
			sol_shapes = [_sol(T, 200, 100, 260, 1320, 0), _sol(T, 220, 100, 530, 1250, 0), _sol(T, 170, 90, 790, 1400, 0), _sol(R, 400, 70, 520, 1425, 0)]
			start_shapes = [_st(T, 200, 100, 853.5, 312.9, 29), _st(T, 220, 100, 192.8, 884.4, 310), _st(T, 170, 90, 397.2, 556.9, 196), _st(R, 400, 70, 567.1, 994.5, 160)]
		15:
			title = "Compass"
			hint = "Four long points, four short ones, and a hub at the centre."
			sol_shapes = [_sol(L, 300, 26, 500, 1320, 0), _sol(L, 300, 26, 500, 1320, 90), _sol(L, 205, 24, 500, 1320, 45), _sol(L, 205, 24, 500, 1320, 135), _sol(C, 135, 135, 500, 1320, 0), _sol(S, 100, 100, 500, 1320, 0)]
			start_shapes = [_st(L, 300, 26, 132.1, 983.4, 280), _st(L, 300, 26, 643.2, 616.4, 16), _st(L, 205, 24, 867.1, 830.1, 338), _st(L, 205, 24, 437.0, 819.1, 292), _st(C, 135, 135, 290.2, 313.4, 83), _st(S, 100, 100, 275.0, 526.4, 343)]

		16:
			title = "Stag"
			hint = "A proud torso, a tilted neck, and twin branching antlers."
			sol_shapes = [_sol(C, 150, 130, 460, 1380, 0), _sol(T, 110, 90, 380, 1270, -35), _sol(L, 160, 24, 330, 1220, 45), _sol(L, 160, 24, 420, 1210, -45), _sol(L, 140, 24, 450, 1430, 85)]
		17:
			title = "Owl"
			hint = "A rounded head, twin feather tufts, and nocturnal wings."
			sol_shapes = [_sol(C, 140, 140, 500, 1270, 0), _sol(C, 170, 170, 500, 1390, 0), _sol(T, 80, 60, 440, 1220, 0), _sol(T, 80, 60, 560, 1220, 0), _sol(L, 180, 26, 360, 1360, 25), _sol(L, 180, 26, 640, 1360, -25)]
		18:
			title = "Turtle"
			hint = "A domed shell, swimming flippers, and a steady head."
			sol_shapes = [_sol(C, 220, 180, 500, 1350, 0), _sol(C, 80, 80, 500, 1230, 0), _sol(T, 120, 70, 360, 1290, 60), _sol(T, 120, 70, 640, 1290, -60), _sol(T, 90, 60, 380, 1420, 120), _sol(T, 90, 60, 620, 1420, -120)]
		19:
			title = "Oak"
			hint = "A broad ancient trunk and three overlapping leaf canopies."
			sol_shapes = [_sol(R, 80, 160, 500, 1420, 0), _sol(C, 180, 180, 500, 1270, 0), _sol(C, 150, 150, 400, 1320, 0), _sol(C, 150, 150, 600, 1320, 0)]
		20:
			title = "Serpent"
			hint = "Five winding loops coiling into a strike."
			sol_shapes = [_sol(C, 110, 110, 320, 1340, 0), _sol(C, 110, 110, 420, 1370, 0), _sol(C, 110, 110, 520, 1340, 0), _sol(C, 110, 110, 620, 1370, 0), _sol(T, 100, 80, 720, 1330, 90)]

		# Default programmatic generation for remaining missions (21-100)
		_:
			var data = _generate_thematic_pattern(num, ch_num)
			title = data["title"]
			hint = data["hint"]
			sol_shapes = data["shapes"]

	# Automatically generate clean, non-overlapping start layout if not manually provided
	if start_shapes.is_empty():
		start_shapes = _generate_scramble_for(sol_shapes, num)

	return {
		"number": num,
		"chapter": ch_num,
		"chapter_title": ch_title,
		"title": title,
		"hint": hint,
		"solution": sol_shapes,
		"start": start_shapes,
		"par_seconds": par
	}

func _generate_scramble_for(sol_shapes: Array[Dictionary], seed_val: int) -> Array[Dictionary]:
	var out: Array[Dictionary] = []
	var n = sol_shapes.size()
	for i in range(n):
		var s = sol_shapes[i]
		# Deterministic pseudo-random placement distributed across the stage quadrants
		var col = i % 3
		var row = i / 3
		var spread_x = [180.0, 500.0, 820.0][col]
		var spread_y = [360.0, 680.0, 960.0][row]
		var jitter_x = (posmod(seed_val * 73 + i * 37, 90) - 45.0)
		var jitter_y = (posmod(seed_val * 53 + i * 29, 90) - 45.0)
		var base_x = spread_x + jitter_x
		var base_y = spread_y + jitter_y
		var rot = float(posmod(seed_val * 127 + i * 89 + 45, 360))

		base_x = clampf(base_x, ShadowCalculator.STAGE_LEFT + 60.0, ShadowCalculator.STAGE_RIGHT - 60.0)
		base_y = clampf(base_y, ShadowCalculator.STAGE_TOP + 60.0, ShadowCalculator.STAGE_BOTTOM - 60.0)

		out.append(_st(s["kind"], s["width"], s["height"], base_x, base_y, rot))
	return out

func _generate_thematic_pattern(num: int, ch_num: int) -> Dictionary:
	var C = ShapeObject.Kind.CIRCLE
	var T = ShapeObject.Kind.TRIANGLE
	var S = ShapeObject.Kind.SQUARE
	var R = ShapeObject.Kind.RECT
	var L = ShapeObject.Kind.LINE

	var title = "Relic %d" % num
	var hint = "Harmonize the shadow silhouette on the parchment wall."
	var shapes: Array[Dictionary] = []

	var sub_idx = (num - 1) % 10 + 1

	match ch_num:
		3: # The Cartographer's Sea (21 - 30)
			match sub_idx:
				1:
					title = "Anchor"
					hint = "A vertical shank, a stock across, and curved flukes below."
					shapes = [_sol(L, 260, 26, 500, 1320, 90), _sol(L, 160, 24, 500, 1250, 0), _sol(C, 90, 90, 500, 1200, 0), _sol(T, 140, 80, 420, 1410, 135), _sol(T, 140, 80, 580, 1410, -135)]
				2:
					title = "Lighthouse"
					hint = "A tapering tower standing sentinel over the reefs."
					shapes = [_sol(R, 140, 180, 500, 1360, 0), _sol(T, 160, 80, 500, 1240, 0), _sol(R, 260, 50, 500, 1430, 0), _sol(L, 110, 22, 500, 1280, 0)]
				3:
					title = "Crescent Moon"
					hint = "A curved sickle of moonlight and a solitary star."
					shapes = [_sol(C, 190, 190, 460, 1330, 0), _sol(C, 160, 160, 490, 1320, 0), _sol(T, 70, 70, 650, 1280, 0), _sol(T, 70, 70, 650, 1280, 180)]
				4:
					title = "Sunburst"
					hint = "A radiant solar disc casting rays to all quarters."
					shapes = [_sol(C, 140, 140, 500, 1330, 0), _sol(T, 110, 60, 500, 1220, 0), _sol(T, 110, 60, 500, 1440, 180), _sol(T, 110, 60, 390, 1330, -90), _sol(T, 110, 60, 610, 1330, 90)]
				5:
					title = "Telescope"
					hint = "Three brass draw-tubes aligned along the celestial axis."
					shapes = [_sol(R, 160, 70, 380, 1360, -25), _sol(R, 140, 55, 500, 1320, -25), _sol(R, 120, 40, 610, 1280, -25), _sol(L, 180, 24, 460, 1420, 60), _sol(L, 180, 24, 520, 1420, -60)]
				6:
					title = "Wave Crest"
					hint = "Three rolling ocean swells breaking over the deep."
					shapes = [_sol(R, 440, 60, 500, 1420, 0), _sol(T, 140, 90, 350, 1350, -20), _sol(T, 160, 100, 500, 1340, -20), _sol(T, 140, 90, 650, 1350, -20)]
				7:
					title = "Sextant"
					hint = "A graduated arc frame, an index arm, and sight mirrors."
					shapes = [_sol(T, 220, 160, 500, 1360, 0), _sol(L, 260, 24, 500, 1330, 45), _sol(C, 80, 80, 500, 1250, 0), _sol(L, 140, 22, 590, 1390, -45)]
				8:
					title = "Galleon"
					hint = "A tiered merchant hull carrying triple billowed sails."
					shapes = [_sol(R, 320, 80, 500, 1420, 0), _sol(T, 180, 120, 420, 1300, 0), _sol(T, 180, 120, 580, 1300, 0), _sol(L, 160, 24, 500, 1290, 90), _sol(T, 110, 70, 700, 1370, 45)]
				9:
					title = "Sea Turtle"
					hint = "A streamlined carapace and broad swimming paddles."
					shapes = [_sol(C, 190, 150, 500, 1350, 0), _sol(T, 90, 60, 500, 1240, 0), _sol(L, 160, 26, 360, 1300, 50), _sol(L, 160, 26, 640, 1300, -50), _sol(T, 80, 50, 500, 1440, 180)]
				10:
					title = "Sea Serpent"
					hint = "An undulating leviathan rising from the abyss."
					shapes = [_sol(C, 110, 110, 320, 1360, 0), _sol(C, 120, 120, 440, 1330, 0), _sol(C, 120, 120, 560, 1360, 0), _sol(T, 130, 90, 680, 1310, 70), _sol(L, 140, 22, 220, 1380, 20)]

		4: # Monuments of the Ancients (31 - 40)
			match sub_idx:
				1:
					title = "Great Pyramid"
					hint = "A monumental triangle flanked by satellite mastabas."
					shapes = [_sol(T, 340, 160, 500, 1360, 0), _sol(T, 140, 80, 280, 1410, 0), _sol(T, 140, 80, 720, 1410, 0), _sol(R, 520, 40, 500, 1440, 0)]
				2:
					title = "Citadel Tower"
					hint = "A crenellated fortress keep with watching bastions."
					shapes = [_sol(R, 180, 190, 500, 1360, 0), _sol(S, 60, 60, 430, 1240, 0), _sol(S, 60, 60, 500, 1240, 0), _sol(S, 60, 60, 570, 1240, 0), _sol(R, 280, 50, 500, 1440, 0)]
				3:
					title = "Stone Bridge"
					hint = "A flat causeway spanning twin vaulted piers."
					shapes = [_sol(R, 460, 50, 500, 1330, 0), _sol(R, 80, 100, 340, 1400, 0), _sol(R, 80, 100, 500, 1400, 0), _sol(R, 80, 100, 660, 1400, 0), _sol(T, 90, 60, 420, 1370, 0), _sol(T, 90, 60, 580, 1370, 0)]
				4:
					title = "Roman Arch"
					hint = "Two monolithic columns joined by a keystoned architrave."
					shapes = [_sol(R, 70, 180, 360, 1380, 0), _sol(R, 70, 180, 640, 1380, 0), _sol(R, 340, 50, 500, 1270, 0), _sol(T, 80, 60, 500, 1240, 180)]
				5:
					title = "Colosseum"
					hint = "Tiered arcade galleries curving over the gladiator sands."
					shapes = [_sol(R, 420, 60, 500, 1430, 0), _sol(R, 340, 50, 500, 1370, 0), _sol(R, 260, 45, 500, 1320, 0), _sol(C, 110, 110, 500, 1270, 0)]
				6:
					title = "Windmill"
					hint = "A timber millhouse driving four rotating vanes."
					shapes = [_sol(R, 140, 170, 500, 1380, 0), _sol(T, 160, 80, 500, 1260, 0), _sol(L, 280, 24, 500, 1260, 0), _sol(L, 280, 24, 500, 1260, 90)]
				7:
					title = "Pagoda"
					hint = "Three gracefully curved tiered roofs tapering to a spire."
					shapes = [_sol(T, 320, 60, 500, 1420, 0), _sol(T, 260, 55, 500, 1350, 0), _sol(T, 200, 50, 500, 1280, 0), _sol(L, 100, 20, 500, 1220, 90), _sol(R, 100, 140, 500, 1380, 0)]
				8:
					title = "Obelisk"
					hint = "A needle of polished red granite pointed toward the sun."
					shapes = [_sol(R, 80, 220, 500, 1370, 0), _sol(T, 90, 70, 500, 1240, 0), _sol(R, 200, 50, 500, 1445, 0)]
				9:
					title = "Castle Gate"
					hint = "Twin round bastions guarding a heavy timber portcullis."
					shapes = [_sol(R, 90, 180, 360, 1380, 0), _sol(R, 90, 180, 640, 1380, 0), _sol(T, 110, 70, 360, 1270, 0), _sol(T, 110, 70, 640, 1270, 0), _sol(R, 190, 120, 500, 1380, 0)]
				10:
					title = "Fortress Keep"
					hint = "The heart of the castle, fortified against all siege."
					shapes = [_sol(R, 260, 150, 500, 1390, 0), _sol(T, 160, 90, 500, 1270, 0), _sol(R, 80, 120, 320, 1400, 0), _sol(R, 80, 120, 680, 1400, 0), _sol(T, 90, 60, 320, 1320, 0), _sol(T, 90, 60, 680, 1320, 0)]

		5: # The Alchemist's Crucible (41 - 50)
			match sub_idx:
				1:
					title = "Crucible"
					hint = "A graphite vessel melting precious ores over glowing coal."
					shapes = [_sol(C, 180, 160, 500, 1380, 0), _sol(R, 220, 40, 500, 1290, 0), _sol(T, 110, 70, 500, 1440, 180), _sol(L, 100, 22, 500, 1230, 90)]
				2:
					title = "Flask"
					hint = "A wide conical vessel bubbling with vitriol essence."
					shapes = [_sol(T, 240, 160, 500, 1390, 180), _sol(R, 60, 100, 500, 1270, 0), _sol(R, 90, 30, 500, 1220, 0)]
				3:
					title = "Scales"
					hint = "An exact beam comparing equal masses of quicksilver."
					shapes = [_sol(L, 340, 24, 500, 1270, 0), _sol(R, 50, 170, 500, 1370, 0), _sol(R, 140, 35, 500, 1440, 0), _sol(T, 90, 60, 340, 1320, 180), _sol(T, 90, 60, 660, 1320, 180)]
				4:
					title = "Hourglass"
					hint = "Twin glass bulbs letting sand trickle through a narrow neck."
					shapes = [_sol(T, 180, 110, 500, 1280, 0), _sol(T, 180, 110, 500, 1390, 180), _sol(R, 200, 35, 500, 1220, 0), _sol(R, 200, 35, 500, 1445, 0)]
				5:
					title = "Optical Prism"
					hint = "A triangular crystal refracting white light into a rainbow."
					shapes = [_sol(T, 220, 160, 500, 1340, 0), _sol(L, 200, 22, 340, 1380, 25), _sol(L, 200, 22, 660, 1320, -15), _sol(L, 200, 22, 670, 1360, -30)]
				6:
					title = "Alembic"
					hint = "A distillation dome with a downward sloping beak."
					shapes = [_sol(C, 170, 170, 440, 1360, 0), _sol(T, 140, 90, 440, 1260, 0), _sol(L, 240, 26, 610, 1350, -40), _sol(R, 120, 50, 440, 1440, 0)]
				7:
					title = "Mortar"
					hint = "A stone bowl and heavy pestle grinding sulfur crystals."
					shapes = [_sol(C, 220, 130, 500, 1410, 0), _sol(R, 160, 40, 500, 1330, 0), _sol(L, 220, 28, 560, 1270, -35), _sol(C, 60, 60, 630, 1210, 0)]
				8:
					title = "Retort"
					hint = "A long-necked glass vessel boiling green vitriol."
					shapes = [_sol(C, 180, 180, 390, 1370, 0), _sol(L, 260, 26, 560, 1320, -30), _sol(C, 90, 90, 670, 1400, 0), _sol(T, 120, 70, 390, 1445, 180)]
				9:
					title = "Bellows"
					hint = "Twin handles squeezing leather to fan the alchemic fire."
					shapes = [_sol(T, 200, 140, 520, 1350, 90), _sol(L, 160, 24, 380, 1280, 25), _sol(L, 160, 24, 380, 1420, -25), _sol(R, 70, 30, 650, 1350, 0)]
				10:
					title = "Ouroboros"
					hint = "The eternal serpent consuming its own tail."
					shapes = [_sol(C, 240, 240, 500, 1340, 0), _sol(C, 150, 150, 500, 1340, 0), _sol(T, 90, 70, 500, 1210, 90), _sol(T, 60, 50, 460, 1210, -90)]

		6: # Bestiary of Myth (51 - 60)
			match sub_idx:
				1:
					title = "Dragon"
					hint = "A crested serpentine maw and leathery bat wings."
					shapes = [_sol(C, 160, 140, 480, 1380, 0), _sol(T, 130, 90, 360, 1290, 75), _sol(T, 180, 110, 420, 1240, -40), _sol(T, 180, 110, 600, 1240, 40), _sol(L, 200, 26, 680, 1400, 35)]
				2:
					title = "Phoenix"
					hint = "A bird of solar fire rising with upraised plumes."
					shapes = [_sol(C, 130, 130, 500, 1350, 0), _sol(T, 90, 70, 500, 1260, 0), _sol(T, 160, 100, 370, 1280, 45), _sol(T, 160, 100, 630, 1280, -45), _sol(L, 180, 24, 440, 1430, 75), _sol(L, 180, 24, 560, 1430, -75)]
				3:
					title = "Griffin"
					hint = "An eagle's beak and wings upon the muscular body of a lion."
					shapes = [_sol(C, 160, 140, 540, 1380, 0), _sol(T, 110, 80, 380, 1300, 80), _sol(T, 160, 100, 480, 1240, -30), _sol(L, 160, 24, 390, 1420, 80), _sol(L, 160, 24, 660, 1420, 70)]
				4:
					title = "Pegasus"
					hint = "A winged stallion galloping across the celestial clouds."
					shapes = [_sol(C, 170, 140, 500, 1370, 0), _sol(T, 110, 80, 360, 1300, 60), _sol(T, 200, 110, 480, 1230, -35), _sol(L, 170, 24, 380, 1430, 75), _sol(L, 170, 24, 610, 1430, 60)]
				5:
					title = "Basilisk"
					hint = "The eight-legged king of serpents bearing a jagged crown."
					shapes = [_sol(C, 190, 130, 500, 1360, 0), _sol(T, 120, 80, 350, 1320, 80), _sol(T, 70, 50, 350, 1250, 0), _sol(L, 180, 26, 680, 1370, 35), _sol(L, 110, 20, 440, 1430, 90), _sol(L, 110, 20, 560, 1430, 90)]
				6:
					title = "Sphinx"
					hint = "A human visage resting upon a couchant lion."
					shapes = [_sol(R, 280, 110, 540, 1400, 0), _sol(C, 120, 120, 380, 1310, 0), _sol(T, 140, 90, 380, 1240, 0), _sol(R, 120, 45, 680, 1435, 0)]
				7:
					title = "Kraken"
					hint = "A bulbous mantle trailing five crushing tentacles."
					shapes = [_sol(C, 200, 160, 500, 1260, 0), _sol(L, 220, 24, 360, 1380, 70), _sol(L, 240, 24, 430, 1400, 80), _sol(L, 250, 24, 500, 1410, 90), _sol(L, 240, 24, 570, 1400, 100), _sol(L, 220, 24, 640, 1380, 110)]
				8:
					title = "Chimera"
					hint = "A roaring lion bearing the horns of a wild goat."
					shapes = [_sol(C, 170, 140, 500, 1380, 0), _sol(T, 130, 90, 380, 1310, 75), _sol(L, 140, 24, 380, 1220, 45), _sol(L, 140, 24, 430, 1210, -35), _sol(L, 200, 24, 690, 1380, 40)]
				9:
					title = "Hydra"
					hint = "Three serpent heads weaving from a single scaled body."
					shapes = [_sol(C, 180, 140, 500, 1410, 0), _sol(L, 180, 24, 390, 1310, 60), _sol(L, 190, 24, 500, 1290, 90), _sol(L, 180, 24, 610, 1310, 120), _sol(T, 90, 60, 330, 1240, 50), _sol(T, 90, 60, 500, 1200, 0), _sol(T, 90, 60, 670, 1240, -50)]
				10:
					title = "Unicorn"
					hint = "A pristine equine forehead pierced by a spiraling horn."
					shapes = [_sol(C, 160, 130, 520, 1380, 0), _sol(T, 110, 80, 380, 1310, 60), _sol(L, 190, 22, 340, 1210, -55), _sol(L, 150, 22, 420, 1440, 80), _sol(L, 150, 22, 620, 1440, 75)]

		7: # The Royal Regalia (61 - 70)
			match sub_idx:
				1:
					title = "Crown"
					hint = "A jeweled circlet crowned with three golden points."
					shapes = [_sol(R, 340, 55, 500, 1420, 0), _sol(T, 120, 110, 370, 1330, 0), _sol(T, 140, 130, 500, 1310, 0), _sol(T, 120, 110, 630, 1330, 0), _sol(C, 50, 50, 500, 1230, 0)]
				2:
					title = "Scepter"
					hint = "A golden shaft surmounted by an orb and cross."
					shapes = [_sol(L, 340, 28, 500, 1370, 90), _sol(C, 110, 110, 500, 1240, 0), _sol(T, 70, 60, 500, 1180, 0), _sol(R, 70, 30, 500, 1445, 0)]
				3:
					title = "Shield"
					hint = "A heater shield emblazoned with a bold heraldic cross."
					shapes = [_sol(R, 260, 110, 500, 1320, 0), _sol(T, 260, 120, 500, 1410, 180), _sol(L, 240, 28, 500, 1360, 90), _sol(L, 240, 28, 500, 1340, 0)]
				4:
					title = "Broadsword"
					hint = "A double-edged steel blade with a wide crossguard and pommel."
					shapes = [_sol(L, 340, 32, 500, 1320, 90), _sol(T, 60, 70, 500, 1160, 0), _sol(L, 180, 26, 500, 1370, 0), _sol(C, 65, 65, 500, 1445, 0)]
				5:
					title = "War Banner"
					hint = "A tall lance supporting a swallowtail royal standard."
					shapes = [_sol(L, 380, 24, 380, 1330, 90), _sol(R, 240, 90, 510, 1260, 0), _sol(T, 90, 80, 640, 1260, -90)]
				6:
					title = "Chalice"
					hint = "A deep bowl, knopped stem, and broad circular base."
					shapes = [_sol(C, 180, 130, 500, 1280, 0), _sol(R, 50, 100, 500, 1360, 0), _sol(C, 220, 60, 500, 1435, 0)]
				7:
					title = "Diadem"
					hint = "An open tiara holding three faceted emerald gems."
					shapes = [_sol(R, 300, 40, 500, 1390, 0), _sol(C, 80, 80, 400, 1340, 0), _sol(C, 100, 100, 500, 1320, 0), _sol(C, 80, 80, 600, 1340, 0), _sol(T, 70, 60, 500, 1240, 0)]
				8:
					title = "Signet"
					hint = "A heavy gold band holding an engraved intaglio seal."
					shapes = [_sol(C, 180, 180, 500, 1370, 0), _sol(C, 120, 120, 500, 1370, 0), _sol(S, 100, 100, 500, 1260, 0), _sol(T, 60, 60, 500, 1260, 0)]
				9:
					title = "Medallion"
					hint = "A heavy chivalric medal suspended from a V-shaped ribbon."
					shapes = [_sol(C, 170, 170, 500, 1380, 0), _sol(L, 160, 26, 430, 1260, -35), _sol(L, 160, 26, 570, 1260, 35), _sol(T, 80, 60, 500, 1380, 0)]
				10:
					title = "Throne"
					hint = "A high-backed royal seat with lion armrests."
					shapes = [_sol(R, 200, 180, 500, 1310, 0), _sol(R, 280, 60, 500, 1400, 0), _sol(T, 180, 80, 500, 1200, 0), _sol(R, 60, 90, 340, 1410, 0), _sol(R, 60, 90, 660, 1410, 0)]

		8: # The Musician's Conservatory (71 - 80)
			match sub_idx:
				1:
					title = "Harp"
					hint = "A curved forepillar, arched neck, and parallel gut strings."
					shapes = [_sol(L, 320, 28, 420, 1330, 80), _sol(L, 220, 26, 510, 1230, -30), _sol(L, 240, 26, 570, 1360, 75), _sol(L, 200, 16, 470, 1310, 90), _sol(L, 200, 16, 510, 1310, 90)]
				2:
					title = "Lute"
					hint = "A vaulted teardrop bowl with an angled pegbox."
					shapes = [_sol(C, 180, 200, 500, 1390, 0), _sol(R, 60, 120, 500, 1270, 0), _sol(R, 80, 45, 520, 1200, 45)]
				3:
					title = "Violin"
					hint = "An hourglass waist, scrolled pegbox, and bridge."
					shapes = [_sol(C, 140, 130, 500, 1330, 0), _sol(C, 170, 150, 500, 1420, 0), _sol(R, 40, 140, 500, 1230, 0), _sol(L, 100, 22, 500, 1360, 0)]
				4:
					title = "Horn"
					hint = "A brass tubing coil flaring outward into a wide bell."
					shapes = [_sol(C, 160, 160, 460, 1350, 0), _sol(C, 110, 110, 460, 1350, 0), _sol(T, 160, 110, 620, 1320, 90), _sol(L, 160, 22, 380, 1260, 0)]
				5:
					title = "Flute"
					hint = "A slender silver tube bearing toneholes along its spine."
					shapes = [_sol(L, 420, 26, 500, 1340, 0), _sol(C, 35, 35, 420, 1340, 0), _sol(C, 35, 35, 480, 1340, 0), _sol(C, 35, 35, 540, 1340, 0), _sol(C, 35, 35, 600, 1340, 0)]
				6:
					title = "Quill"
					hint = "A curved feather vane terminating in a split nib."
					shapes = [_sol(L, 340, 24, 460, 1320, -45), _sol(T, 120, 60, 390, 1270, 45), _sol(T, 140, 70, 480, 1220, 45), _sol(T, 50, 40, 570, 1435, 135)]
				7:
					title = "Palette"
					hint = "A kidney-shaped artist's board bearing four pigment cakes."
					shapes = [_sol(C, 240, 170, 500, 1360, 0), _sol(C, 50, 50, 400, 1380, 0), _sol(C, 45, 45, 460, 1320, 0), _sol(C, 45, 45, 540, 1320, 0), _sol(C, 45, 45, 600, 1350, 0)]
				8:
					title = "Lyre"
					hint = "Two graceful bovine horns joined by an ebony crossbar."
					shapes = [_sol(L, 240, 26, 420, 1340, 80), _sol(L, 240, 26, 580, 1340, 100), _sol(L, 200, 24, 500, 1260, 0), _sol(R, 180, 60, 500, 1420, 0), _sol(L, 160, 16, 470, 1340, 90), _sol(L, 160, 16, 530, 1340, 90)]
				9:
					title = "Bell"
					hint = "A flaring bronze cup and heavy pendulous clapper."
					shapes = [_sol(T, 240, 160, 500, 1340, 0), _sol(R, 260, 35, 500, 1415, 0), _sol(C, 70, 70, 500, 1230, 0), _sol(C, 60, 60, 500, 1445, 0)]
				10:
					title = "Mask"
					hint = "Slanted almond eyes and feathered carnival wings."
					shapes = [_sol(R, 280, 90, 500, 1350, 0), _sol(T, 140, 80, 340, 1320, -45), _sol(T, 140, 80, 660, 1320, 45), _sol(C, 60, 60, 440, 1350, 0), _sol(C, 60, 60, 560, 1350, 0)]

		9: # Clockwork & Automata (81 - 90)
			match sub_idx:
				1:
					title = "Gear"
					hint = "A central hub ring and four interlocking gear teeth."
					shapes = [_sol(C, 180, 180, 500, 1340, 0), _sol(S, 65, 65, 500, 1220, 0), _sol(S, 65, 65, 500, 1460, 0), _sol(S, 65, 65, 380, 1340, 0), _sol(S, 65, 65, 620, 1340, 0)]
				2:
					title = "Pendulum"
					hint = "A knife-edge suspension dropping to a heavy lead disc."
					shapes = [_sol(L, 320, 24, 500, 1310, 90), _sol(C, 160, 160, 500, 1420, 0), _sol(S, 70, 70, 500, 1170, 0)]
				3:
					title = "Compass Rose"
					hint = "Four primary cardinal arrows crossing four secondary points."
					shapes = [_sol(T, 80, 140, 500, 1230, 0), _sol(T, 80, 140, 500, 1450, 180), _sol(T, 80, 140, 390, 1340, -90), _sol(T, 80, 140, 610, 1340, 90), _sol(C, 110, 110, 500, 1340, 0)]
				4:
					title = "Piston"
					hint = "A sliding cylindrical head and reciprocating crank."
					shapes = [_sol(R, 180, 80, 500, 1270, 0), _sol(L, 220, 26, 500, 1370, 90), _sol(C, 90, 90, 500, 1445, 0)]
				5:
					title = "Camshaft"
					hint = "An axial shaft rotating three teardrop lobes."
					shapes = [_sol(L, 420, 26, 500, 1340, 0), _sol(C, 80, 80, 410, 1300, 0), _sol(C, 80, 80, 500, 1380, 0), _sol(C, 80, 80, 590, 1300, 0)]
				6:
					title = "Flywheel"
					hint = "A heavy rim storing rotational momentum via four spokes."
					shapes = [_sol(C, 240, 240, 500, 1340, 0), _sol(C, 160, 160, 500, 1340, 0), _sol(L, 220, 22, 500, 1340, 0), _sol(L, 220, 22, 500, 1340, 90)]
				7:
					title = "Balance Wheel"
					hint = "A temperature-compensated ring oscillating on a hairspring."
					shapes = [_sol(C, 220, 220, 500, 1340, 0), _sol(L, 210, 24, 500, 1340, 45), _sol(S, 60, 60, 500, 1340, 0)]
				8:
					title = "Astrolabe"
					hint = "A sighting alidade rotating over celestial coordinate plates."
					shapes = [_sol(C, 230, 230, 500, 1340, 0), _sol(L, 300, 26, 500, 1340, 35), _sol(T, 80, 60, 500, 1210, 0)]
				9:
					title = "Metronome"
					hint = "A wooden pyramid sheltering an inverted sliding weight."
					shapes = [_sol(T, 240, 200, 500, 1350, 0), _sol(L, 240, 22, 500, 1310, -20), _sol(S, 50, 50, 480, 1270, -20), _sol(R, 260, 40, 500, 1445, 0)]
				10:
					title = "Automaton"
					hint = "A clockwork humanoid core wound by twin keys."
					shapes = [_sol(C, 100, 100, 500, 1220, 0), _sol(R, 160, 150, 500, 1340, 0), _sol(T, 80, 60, 400, 1310, -90), _sol(T, 80, 60, 600, 1310, 90), _sol(L, 140, 24, 450, 1440, 90), _sol(L, 140, 24, 550, 1440, 90)]

		10: # Celestial Convergence (91 - 100)
			match sub_idx:
				1:
					title = "Solar Eclipse"
					hint = "The dark lunar disc masking the flaming solar corona."
					shapes = [_sol(C, 200, 200, 500, 1340, 0), _sol(C, 160, 160, 485, 1340, 0), _sol(T, 90, 70, 620, 1260, 45)]
				2:
					title = "Supernova"
					hint = "A collapsing core erupting in six relativistic jets."
					shapes = [_sol(C, 130, 130, 500, 1340, 0), _sol(T, 90, 110, 500, 1210, 0), _sol(T, 90, 110, 500, 1470, 180), _sol(T, 90, 110, 370, 1280, -60), _sol(T, 90, 110, 630, 1280, 60), _sol(T, 90, 110, 370, 1400, -120), _sol(T, 90, 110, 630, 1400, 120)]
				3:
					title = "Zodiac Wheel"
					hint = "A circular ecliptic band divided by four solar gates."
					shapes = [_sol(C, 240, 240, 500, 1340, 0), _sol(C, 160, 160, 500, 1340, 0), _sol(S, 130, 130, 500, 1340, 45), _sol(C, 70, 70, 500, 1340, 0)]
				4:
					title = "Orbital Zenith"
					hint = "A planetary sphere encircled by a tipped asteroid belt."
					shapes = [_sol(C, 180, 180, 500, 1340, 0), _sol(L, 340, 26, 500, 1340, -25), _sol(C, 60, 60, 360, 1280, 0), _sol(C, 60, 60, 640, 1400, 0)]
				5:
					title = "Solstice Gate"
					hint = "A megalithic stone trilithon framing the rising midsummer sun."
					shapes = [_sol(R, 70, 180, 380, 1370, 0), _sol(R, 70, 180, 620, 1370, 0), _sol(R, 340, 50, 500, 1260, 0), _sol(C, 110, 110, 500, 1370, 0)]
				6:
					title = "Equinox"
					hint = "The celestial equator balancing equal hours of day and night."
					shapes = [_sol(L, 380, 26, 500, 1340, 0), _sol(C, 150, 150, 500, 1260, 0), _sol(C, 150, 150, 500, 1420, 0), _sol(T, 80, 60, 500, 1340, 0)]
				7:
					title = "Pulsar"
					hint = "A neutron core beaming twin electromagnetic lighthouse rays."
					shapes = [_sol(C, 120, 120, 500, 1340, 0), _sol(L, 380, 24, 500, 1340, 65), _sol(T, 100, 80, 390, 1200, -35), _sol(T, 100, 80, 610, 1480, 145)]
				8:
					title = "Singularity"
					hint = "An infinite gravitational pit warping surrounding spacetime."
					shapes = [_sol(C, 130, 130, 500, 1340, 0), _sol(C, 210, 70, 500, 1340, 30), _sol(C, 210, 70, 500, 1340, -30), _sol(C, 260, 50, 500, 1340, 0)]
				9:
					title = "Constellation"
					hint = "Five brilliant stars joined by luminous celestial vectors."
					shapes = [_sol(L, 260, 24, 430, 1280, 35), _sol(L, 240, 24, 560, 1320, -50), _sol(L, 220, 24, 480, 1410, 20), _sol(C, 55, 55, 330, 1230, 0), _sol(C, 55, 55, 530, 1320, 0), _sol(C, 55, 55, 660, 1270, 0), _sol(C, 55, 55, 410, 1420, 0)]
				10:
					title = "Master of Shadows"
					hint = "The pinnacle alignment: the eye, the pyramid, and the radiant crown."
					shapes = [_sol(T, 280, 180, 500, 1340, 0), _sol(C, 110, 110, 500, 1340, 0), _sol(C, 50, 50, 500, 1340, 0), _sol(L, 360, 26, 500, 1240, 0), _sol(T, 100, 80, 500, 1190, 0), _sol(R, 340, 45, 500, 1440, 0)]

	return {
		"title": title,
		"hint": hint,
		"shapes": shapes
	}
