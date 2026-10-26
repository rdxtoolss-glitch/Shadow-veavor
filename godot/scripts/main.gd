class_name Main
extends Node2D

@onready var game_stage: GameStage = $GameStage
@onready var wall_screen: WallScreen = $WallScreen
@onready var hud: HUD = $HUD
@onready var level_complete_modal: LevelCompleteModal = $LevelCompleteModal
@onready var pause_modal: PauseModal = $PauseModal
@onready var tokens_container: Node2D = $TokensContainer
@onready var audio_manager: AudioManager = $AudioManager

var current_level_idx: int = 0
var matcher: SilhouetteMatcher = SilhouetteMatcher.new()
var active_token: ShapeToken = null
var live_tokens: Array[ShapeToken] = []

var level_start_time: float = 0.0
var level_is_complete: bool = false
var token_scene = preload("res://scenes/shape_token.tscn")

func _ready():
	hud.pause_pressed.connect(_on_pause_pressed)
	hud.reset_pressed.connect(_on_reset_pressed)
	level_complete_modal.next_pressed.connect(_on_next_level_pressed)
	level_complete_modal.replay_pressed.connect(_on_replay_pressed)
	pause_modal.resume_pressed.connect(_on_resume_pressed)
	pause_modal.restart_pressed.connect(_on_reset_pressed)
	pause_modal.sound_toggled.connect(_on_sound_toggled)

	load_level(current_level_idx)

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
	hud.update_level_info(lv.number, lv.title, lv.hint)

	# 2. Forge and set target on wall
	var target_polys = LevelLoader.forge_target(current_level_idx)
	matcher.set_target(target_polys)
	wall_screen.set_target(target_polys)

	# 3. Create start shapes
	var start_shapes = LevelLoader.create_shapes(current_level_idx)
	for data in start_shapes:
		var token = token_scene.instantiate() as ShapeToken
		tokens_container.add_child(token)
		token.setup(data)
		live_tokens.append(token)

	_evaluate_shadows()

func _input(event: InputEvent):
	if level_is_complete: return
	if pause_modal.visible or level_complete_modal.visible: return

	if event is InputEventMouseButton:
		if event.button_index == MOUSE_BUTTON_LEFT:
			if event.pressed:
				_handle_press(event.position)
			else:
				_handle_release()
	elif event is InputEventMouseMotion:
		if active_token and (active_token.shape_data.is_dragging or active_token.shape_data.is_rotating):
			active_token.update_drag(event.position)
			_evaluate_shadows()
	elif event is InputEventScreenTouch:
		if event.pressed:
			_handle_press(event.position)
		else:
			_handle_release()
	elif event is InputEventScreenDrag:
		if active_token and (active_token.shape_data.is_dragging or active_token.shape_data.is_rotating):
			active_token.update_drag(event.position)
			_evaluate_shadows()

func _handle_press(pos: Vector2):
	# First check rotation handle on active token if already selected
	if active_token and active_token.is_on_rotation_handle(pos):
		active_token.start_handle_drag(pos)
		audio_manager.play_pick()
		return

	# Search top-to-bottom among tokens
	var hit: ShapeToken = null
	for i in range(live_tokens.size() - 1, -1, -1):
		var t = live_tokens[i]
		if t.contains_point(pos):
			hit = t
			break

	if hit:
		if active_token and active_token != hit:
			active_token.is_selected = false
		active_token = hit
		active_token.is_selected = true
		# Bring to top in scene tree
		tokens_container.move_child(active_token, tokens_container.get_child_count() - 1)
		active_token.start_drag(pos)
		audio_manager.play_pick()
	else:
		if active_token:
			active_token.is_selected = false
			active_token = null

func _handle_release():
	if active_token:
		active_token.finish_drag()
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
	var pct: float = matcher.reportMatch(raw_iou) if matcher.has_method("reportMatch") else matcher.report_match(raw_iou)

	wall_screen.update_shadows(shadow_detailed, raw_iou)
	hud.update_resonance(pct)

	# Sound chimes
	var chimes = matcher.newly_crossed_chimes(raw_iou)
	if chimes & 1: audio_manager.play_resonance_chime(1)
	if chimes & 2: audio_manager.play_resonance_chime(2)
	if chimes & 4: audio_manager.play_resonance_chime(3)

	# Level win condition
	if matcher.is_complete(raw_iou) and not level_is_complete:
		level_is_complete = true
		var duration = (Time.get_ticks_msec() / 1000.0) - level_start_time
		var stars = 3 if duration < 25.0 else (2 if duration < 50.0 else 1)
		audio_manager.play_resonance_chime(3)
		get_tree().create_timer(0.4).timeout.connect(func():
			level_complete_modal.show_completion(current_level_idx + 1, duration, stars)
		)

func _on_pause_pressed():
	pause_modal.open()

func _on_resume_pressed():
	pass

func _on_reset_pressed():
	load_level(current_level_idx)

func _on_next_level_pressed():
	level_complete_modal.hide()
	var next_idx = (current_level_idx + 1) % LevelLoader.count()
	load_level(next_idx)

func _on_replay_pressed():
	level_complete_modal.hide()
	load_level(current_level_idx)

func _on_sound_toggled(enabled: bool):
	audio_manager.sound_enabled = enabled
