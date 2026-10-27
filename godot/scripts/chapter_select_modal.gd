class_name ChapterSelectModal
extends Control

const SaveManager = preload("res://scripts/save_manager.gd")

signal mission_selected(level_idx: int)
signal closed

const CHAPTER_COUNT: int = 10
const MISSIONS_PER_CHAPTER: int = 10

var current_chapter: int = 1 # 1-based (1 to 10)
var audio_manager: AudioManager = null

@onready var backdrop: ColorRect = $Backdrop
@onready var panel: Panel = $MainPanel
@onready var close_btn: Button = $MainPanel/Header/CloseButton
@onready var total_stars_label: Label = $MainPanel/Header/TotalStarsPill/StarsLabel
@onready var chapter_title_label: Label = $MainPanel/ChapterNav/ChapterTitle
@onready var chapter_stats_label: Label = $MainPanel/ChapterNav/ChapterStats
@onready var prev_ch_btn: Button = $MainPanel/ChapterNav/PrevButton
@onready var next_ch_btn: Button = $MainPanel/ChapterNav/NextButton
@onready var tabs_container: HBoxContainer = $MainPanel/ChapterTabsScroll/TabsContainer
@onready var missions_container: VBoxContainer = $MainPanel/MissionsScroll/MissionsContainer
@onready var unlock_all_btn: Button = $MainPanel/Footer/UnlockAllButton

const CHAPTER_NAMES: Array[String] = [
	"Genesis",
	"Sacred Geometry",
	"Clockwork Automata",
	"Bestiary of Nyx",
	"Arcane Apparatus",
	"Celestial Navigation",
	"The Alchemist's Study",
	"Architecture of Shadows",
	"Optical Enigmas",
	"Master Weaver's Trial"
]

const ROMAN_NUMERALS: Array[String] = [
	"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
]

func _ready():
	hide()
	close_btn.pressed.connect(_on_close_pressed)
	prev_ch_btn.pressed.connect(_on_prev_chapter)
	next_ch_btn.pressed.connect(_on_next_chapter)
	unlock_all_btn.pressed.connect(_on_toggle_unlock_all)
	_build_chapter_tabs()

func _build_chapter_tabs():
	for child in tabs_container.get_children():
		child.queue_free()

	for i in range(CHAPTER_COUNT):
		var ch_num = i + 1
		var tab_btn = Button.new()
		tab_btn.text = "Ch %s" % ROMAN_NUMERALS[i]
		tab_btn.custom_minimum_size = Vector2(80, 42)
		tab_btn.mouse_default_cursor_shape = Control.CURSOR_POINTING_HAND
		tab_btn.pressed.connect(func(): select_chapter(ch_num))
		tabs_container.add_child(tab_btn)

func open(target_chapter: int = 1):
	current_chapter = clampi(target_chapter, 1, CHAPTER_COUNT)
	show()
	_refresh_ui()
	if audio_manager:
		audio_manager.play_parchment_open()

	# Emil Kowalski entrance: start scale 0.96, cubic ease out
	panel.scale = Vector2(0.96, 0.96)
	panel.modulate.a = 0.0
	backdrop.modulate.a = 0.0

	var tw = create_tween().set_parallel(true)
	tw.tween_property(backdrop, "modulate:a", 1.0, 0.22)
	tw.tween_property(panel, "scale", Vector2(1.0, 1.0), 0.26).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_OUT)
	tw.tween_property(panel, "modulate:a", 1.0, 0.22)

func close():
	if audio_manager:
		audio_manager.play_parchment_close()

	var tw = create_tween().set_parallel(true)
	tw.tween_property(backdrop, "modulate:a", 0.0, 0.18)
	tw.tween_property(panel, "scale", Vector2(0.96, 0.96), 0.18).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_IN)
	tw.tween_property(panel, "modulate:a", 0.0, 0.18)
	tw.finished.connect(func():
		hide()
		closed.emit()
	)

func _on_close_pressed():
	if audio_manager:
		audio_manager.play_ui_tap()
	close()

func _on_prev_chapter():
	select_chapter(maxi(1, current_chapter - 1))

func _on_next_chapter():
	select_chapter(mini(CHAPTER_COUNT, current_chapter + 1))

func _on_toggle_unlock_all():
	if audio_manager:
		audio_manager.play_ui_tap()
	var unlocked = SaveManager.toggle_unlock_all()
	_refresh_ui()

func select_chapter(ch_num: int):
	var clamped = clampi(ch_num, 1, CHAPTER_COUNT)
	if clamped != current_chapter and audio_manager:
		audio_manager.play_parchment_flip()
	current_chapter = clamped
	_refresh_ui()

func _refresh_ui():
	var total_stars = SaveManager.get_total_stars()
	total_stars_label.text = "★ %d / 300" % total_stars

	if SaveManager.debug_unlock_all:
		unlock_all_btn.text = "DEV MODE: ALL 100 UNLOCKED (ACTIVE)"
		unlock_all_btn.modulate = Color(1.0, 0.85, 0.35, 1.0)
	else:
		unlock_all_btn.text = "DEV MODE: UNLOCK ALL MISSIONS"
		unlock_all_btn.modulate = Color(0.75, 0.75, 0.75, 0.9)

	var ch_idx = current_chapter - 1
	chapter_title_label.text = "CHAPTER %s: %s" % [ROMAN_NUMERALS[ch_idx], CHAPTER_NAMES[ch_idx].to_upper()]

	var ch_stars = SaveManager.get_chapter_stars(current_chapter)
	var ch_completed = SaveManager.get_chapter_completed_count(current_chapter)
	chapter_stats_label.text = "%d / 10 Completed  •  %d / 30 ★" % [ch_completed, ch_stars]

	prev_ch_btn.disabled = (current_chapter <= 1)
	next_ch_btn.disabled = (current_chapter >= CHAPTER_COUNT)

	# Update chapter tabs highlight
	var tabs = tabs_container.get_children()
	for i in range(tabs.size()):
		var t = tabs[i] as Button
		if i + 1 == current_chapter:
			t.modulate = Color(1.0, 0.85, 0.35, 1.0)
		else:
			t.modulate = Color(0.65, 0.65, 0.65, 0.8)

	# Populate 10 missions for current chapter
	_populate_missions()

func _populate_missions():
	for child in missions_container.get_children():
		child.queue_free()

	var start_idx = (current_chapter - 1) * MISSIONS_PER_CHAPTER
	for i in range(MISSIONS_PER_CHAPTER):
		var level_idx = start_idx + i
		var level_num = level_idx + 1
		if level_idx >= LevelLoader.count():
			break

		var lv = LevelLoader.get_level(level_idx)
		var is_unlocked = SaveManager.is_level_unlocked(level_num)
		var stars = SaveManager.get_level_stars(level_num)
		var best_time = SaveManager.get_level_best_time(level_num)

		var card = _create_mission_card(level_idx, lv, is_unlocked, stars, best_time)
		missions_container.add_child(card)

func _create_mission_card(level_idx: int, lv: LevelData, is_unlocked: bool, stars: int, best_time: float) -> PanelContainer:
	var card = PanelContainer.new()
	card.custom_minimum_size = Vector2(860, 84)

	var style = StyleBoxFlat.new()
	style.corner_radius_top_left = 12
	style.corner_radius_top_right = 12
	style.corner_radius_bottom_right = 12
	style.corner_radius_bottom_left = 12

	if not is_unlocked:
		style.bg_color = Color(0.10, 0.08, 0.07, 0.75)
		style.border_color = Color(0.28, 0.22, 0.18, 0.5)
	elif stars > 0:
		style.bg_color = Color(0.16, 0.12, 0.08, 0.95)
		style.border_color = Color(0.72, 0.58, 0.28, 0.9)
	else:
		style.bg_color = Color(0.14, 0.11, 0.09, 0.92)
		style.border_color = Color(0.48, 0.40, 0.30, 0.7)

	style.border_width_left = 2
	style.border_width_top = 2
	style.border_width_right = 2
	style.border_width_bottom = 2
	card.add_theme_stylebox_override("panel", style)

	var hbox = HBoxContainer.new()
	hbox.add_theme_constant_override("separation", 18)
	card.add_child(hbox)

	# Left space
	var pad_left = Control.new()
	pad_left.custom_minimum_size = Vector2(12, 0)
	hbox.add_child(pad_left)

	# Level number pill
	var num_label = Label.new()
	num_label.text = "#%d" % lv.number
	num_label.custom_minimum_size = Vector2(54, 40)
	num_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	num_label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	num_label.add_theme_font_size_override("font_size", 20)
	num_label.modulate = Color(0.95, 0.82, 0.52, 1.0) if is_unlocked else Color(0.45, 0.40, 0.35, 0.6)
	hbox.add_child(num_label)

	# Center info VBox: Title, shapes, best time
	var info_vbox = VBoxContainer.new()
	info_vbox.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	info_vbox.alignment = BoxContainer.ALIGNMENT_CENTER

	var title_label = Label.new()
	title_label.text = lv.title.to_upper()
	title_label.add_theme_font_size_override("font_size", 20)
	title_label.modulate = Color(0.96, 0.92, 0.85, 1.0) if is_unlocked else Color(0.50, 0.45, 0.40, 0.6)
	info_vbox.add_child(title_label)

	var meta_label = Label.new()
	var shape_count = lv.shape_count()
	var diff_str = "Novice" if shape_count <= 2 else ("Adept" if shape_count == 3 else ("Master" if shape_count == 4 else "Grandmaster"))
	var time_str = ("Best: %.1fs" % best_time) if (best_time > 0.0) else ("Par: %ds" % int(lv.par_seconds))
	meta_label.text = "%d Tokens  •  %s  •  %s" % [shape_count, diff_str, time_str]
	meta_label.add_theme_font_size_override("font_size", 14)
	meta_label.modulate = Color(0.65, 0.58, 0.50, 0.85) if is_unlocked else Color(0.40, 0.35, 0.30, 0.5)
	info_vbox.add_child(meta_label)
	hbox.add_child(info_vbox)

	# Stars display
	var stars_hbox = HBoxContainer.new()
	stars_hbox.alignment = BoxContainer.ALIGNMENT_CENTER
	stars_hbox.custom_minimum_size = Vector2(100, 0)
	for s in range(3):
		var star_label = Label.new()
		star_label.text = "★" if s < stars else "☆"
		star_label.add_theme_font_size_override("font_size", 22)
		if s < stars:
			star_label.modulate = Color(1.0, 0.85, 0.30, 1.0)
		else:
			star_label.modulate = Color(0.35, 0.30, 0.25, 0.5)
		stars_hbox.add_child(star_label)
	hbox.add_child(stars_hbox)

	# Play / Locked Button
	var action_btn = Button.new()
	action_btn.custom_minimum_size = Vector2(130, 48)
	action_btn.size_flags_vertical = Control.SIZE_SHRINK_CENTER

	var btn_style = StyleBoxFlat.new()
	btn_style.corner_radius_top_left = 8
	btn_style.corner_radius_top_right = 8
	btn_style.corner_radius_bottom_right = 8
	btn_style.corner_radius_bottom_left = 8

	if not is_unlocked:
		action_btn.text = "🔒 LOCKED"
		action_btn.disabled = true
		btn_style.bg_color = Color(0.20, 0.16, 0.14, 0.6)
		action_btn.modulate = Color(0.55, 0.50, 0.45, 0.7)
	else:
		action_btn.text = "REPLAY ▶" if stars > 0 else "PLAY ▶"
		action_btn.disabled = false
		btn_style.bg_color = Color(0.85, 0.65, 0.25, 1.0) if stars == 0 else Color(0.35, 0.26, 0.16, 1.0)
		action_btn.modulate = Color(0.12, 0.08, 0.04, 1.0) if stars == 0 else Color(0.92, 0.82, 0.60, 1.0)
		action_btn.mouse_default_cursor_shape = Control.CURSOR_POINTING_HAND
		action_btn.pressed.connect(func(): _on_select_mission(level_idx))

	action_btn.add_theme_stylebox_override("normal", btn_style)
	hbox.add_child(action_btn)

	# Right padding
	var pad_right = Control.new()
	pad_right.custom_minimum_size = Vector2(12, 0)
	hbox.add_child(pad_right)

	return card

func _on_select_mission(idx: int):
	if audio_manager:
		audio_manager.play_ui_tap()
	close()
	mission_selected.emit(idx)
