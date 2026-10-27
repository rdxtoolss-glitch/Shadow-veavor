class_name Main
extends Node

@onready var world_3d: GameWorld3D = $SubViewportContainer/SubViewport/World3D
@onready var sub_viewport: SubViewport = $SubViewportContainer/SubViewport
@onready var wall_screen: WallScreen = $WallScreen
@onready var hud: HUD = $HUD
@onready var level_complete_modal: LevelCompleteModal = $LevelCompleteModal
@onready var pause_modal: PauseModal = $PauseModal
const ChapterSelectModal = preload("res://scripts/chapter_select_modal.gd")
@onready var chapter_select_modal: Control = $ChapterSelectModal
@onready var audio_manager: AudioManager = $AudioManager

var current_level_idx: int = 0
var matcher: SilhouetteMatcher = SilhouetteMatcher.new()
var active_token: Token3D = null
var live_tokens: Array[Token3D] = []

var level_start_time: float = 0.0
var level_is_complete: bool = false
const Token3DScript = preload("res://scripts/token_3d.gd")
const SaveManager = preload("res://scripts/save_manager.gd")

func _ready():
	SaveManager.init_if_needed()
	audio_manager.sound_enabled = SaveManager.sound_enabled
	audio_manager.haptics_enabled = SaveManager.haptics_enabled

	hud.pause_pressed.connect(_on_pause_pressed)
	hud.reset_pressed.connect(_on_reset_pressed)
	hud.codex_pressed.connect(_on_codex_pressed)

	level_complete_modal.next_pressed.connect(_on_next_level_pressed)
	level_complete_modal.replay_pressed.connect(_on_replay_pressed)

	pause_modal.resume_pressed.connect(_on_resume_pressed)
	pause_modal.restart_pressed.connect(_on_reset_pressed)
	pause_modal.sound_toggled.connect(_on_sound_toggled)

	chapter_select_modal.audio_manager = audio_manager
	chapter_select_modal.mission_selected.connect(_on_mission_selected_from_codex)

	# Start on the highest unlocked mission if valid, or Level 1
	var start_idx = clampi(SaveManager.unlocked_max - 1, 0, LevelLoader.count() - 1)
	load_level(start_idx)

func load_level(idx: int):
	current_level_idx = clampi(idx, 0, LevelLoader.count() - 1)
	level_is_complete = false
	active_token = null
	level_start_time = Time.get_ticks_msec() / 1000.0

	# 1. Clean existing tokens
	for t in live_tokens:
		if is_instance_valid(t):
			t.queue_free()
	live_tokens.clear()

	var lv = LevelLoader.get_level(current_level_idx)
	hud.update_level_info(lv.number, lv.title, lv.hint, lv.chapter, lv.chapter_title)

	# 2. Forge target on wall screen
	var target_polys = LevelLoader.forge_target(current_level_idx)
	matcher.set_target(target_polys)
	wall_screen.set_target(target_polys)

	# 3. Create 3D physical tokens
	var start_shapes = LevelLoader.create_shapes(current_level_idx)
	for data in start_shapes:
		var token = Token3DScript.new() as Token3D
		world_3d.tokens_root.add_child(token)
		token.setup(data)
		token.gear_ticked.connect(func(_t): audio_manager.play_gear_tick())
		token.detent_snapped.connect(func(_t): audio_manager.play_detent())
		live_tokens.append(token)

	_evaluate_shadows()

func _input(event: InputEvent):
	if level_is_complete: return
	if pause_modal.visible or level_complete_modal.visible or chapter_select_modal.visible: return

	# Normalize screen coordinate for subtle 3D camera parallax
	if event is InputEventMouseMotion or event is InputEventScreenDrag:
		var vp_size = sub_viewport.size
		var norm_pos = Vector2(
			(event.position.x / float(vp_size.x)) * 2.0 - 1.0,
			(event.position.y / float(vp_size.y)) * 2.0 - 1.0
		)
		world_3d.set_parallax_target(norm_pos)

	if event is InputEventMouseButton:
		if event.button_index == MOUSE_BUTTON_LEFT:
			if event.pressed:
				_handle_press(event.position)
			else:
				_handle_release()
	elif event is InputEventMouseMotion:
		if active_token and (active_token.shape_data.is_dragging or active_token.shape_data.is_rotating):
			var ray = _get_3d_ray(event.position)
			active_token.update_drag_ray(ray.origin, ray.direction)
			if active_token.shape_data.is_dragging:
				var spd = event.velocity.length() if "velocity" in event else (event.relative.length() * 60.0)
				audio_manager.update_slide(spd)
			_evaluate_shadows()
	elif event is InputEventScreenTouch:
		if event.pressed:
			_handle_press(event.position)
		else:
			_handle_release()
	elif event is InputEventScreenDrag:
		if active_token and (active_token.shape_data.is_dragging or active_token.shape_data.is_rotating):
			var ray = _get_3d_ray(event.position)
			active_token.update_drag_ray(ray.origin, ray.direction)
			if active_token.shape_data.is_dragging:
				var spd = event.velocity.length() if "velocity" in event else (event.relative.length() * 60.0)
				audio_manager.update_slide(spd)
			_evaluate_shadows()

func _get_3d_ray(screen_pos: Vector2) -> Dictionary:
	var cam = world_3d.camera
	var origin = cam.project_ray_origin(screen_pos)
	var dir = cam.project_ray_normal(screen_pos)
	return {"origin": origin, "direction": dir}

func _handle_press(pos: Vector2):
	var ray = _get_3d_ray(pos)

	# 1. Check active token rotation handle
	if active_token and active_token.is_on_rotation_handle(ray.origin, ray.direction):
		active_token.start_handle_drag(ray.origin, ray.direction)
		audio_manager.play_pick()
		return

	# 2. Check token bodies in 3D
	var hit: Token3D = null
	for i in range(live_tokens.size() - 1, -1, -1):
		var t = live_tokens[i]
		if t.contains_ray(ray.origin, ray.direction):
			hit = t
			break

	if hit:
		if active_token and active_token != hit:
			active_token.is_selected = false
		active_token = hit
		active_token.is_selected = true
		active_token.start_drag(ray.origin, ray.direction)
		audio_manager.play_pick()
		audio_manager.start_slide()
	else:
		if active_token:
			active_token.is_selected = false
			active_token = null

func _handle_release():
	if active_token:
		active_token.finish_drag()
		audio_manager.stop_slide()
		audio_manager.play_drop()

func _evaluate_shadows():
	var shadow_polys: Array[PackedVector2Array] = []
	var shadow_detailed: Array[Dictionary] = []

	for t in live_tokens:
		if not t or not t.shape_data: continue
		var detailed = ShadowCalculator.project_detailed(t.shape_data)
		shadow_detailed.append(detailed)
		shadow_polys.append(detailed["core"])

	var raw_iou: float = matcher.evaluate(shadow_polys)
	var pct: float = matcher.report_match(raw_iou)

	wall_screen.update_shadows(shadow_detailed, raw_iou)
	hud.update_resonance(pct)
	audio_manager.update_resonance_alignment(raw_iou)

	# Chime milestones
	var chimes = matcher.newly_crossed_chimes(raw_iou)
	if chimes & 1: audio_manager.play_resonance_chime(1)
	if chimes & 2: audio_manager.play_resonance_chime(2)
	if chimes & 4: audio_manager.play_resonance_chime(3)

	# Win condition
	if matcher.is_complete(raw_iou) and not level_is_complete:
		level_is_complete = true
		var duration = (Time.get_ticks_msec() / 1000.0) - level_start_time
		var lv = LevelLoader.get_level(current_level_idx)
		var par = float(lv.par_seconds)
		var stars = 3 if duration <= par else (2 if duration <= par * 1.5 else 1)

		SaveManager.record_completion(lv.number, duration, stars)
		audio_manager.stop_slide()
		audio_manager.play_victory()

		# Cinematic push-in on 3D camera
		var cam = world_3d.camera
		var tw = create_tween()
		tw.tween_property(cam, "fov", 42.0, 0.4).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_OUT)
		tw.tween_property(cam, "fov", 46.0, 0.5)

		get_tree().create_timer(0.45).timeout.connect(func():
			level_complete_modal.show_completion(lv.number, duration, stars, audio_manager)
		)

func _on_pause_pressed():
	audio_manager.play_ui_tap()
	pause_modal.open()

func _on_resume_pressed():
	audio_manager.play_ui_tap()

func _on_reset_pressed():
	audio_manager.play_ui_tap()
	load_level(current_level_idx)

func _on_codex_pressed():
	var lv = LevelLoader.get_level(current_level_idx)
	chapter_select_modal.open(lv.chapter)

func _on_mission_selected_from_codex(idx: int):
	load_level(idx)

func _on_next_level_pressed():
	audio_manager.play_ui_tap()
	level_complete_modal.hide()
	var next_idx = (current_level_idx + 1) % LevelLoader.count()
	load_level(next_idx)

func _on_replay_pressed():
	audio_manager.play_ui_tap()
	level_complete_modal.hide()
	load_level(current_level_idx)

func _on_sound_toggled(enabled: bool):
	audio_manager.sound_enabled = enabled
	SaveManager.sound_enabled = enabled
	SaveManager.save_data()
