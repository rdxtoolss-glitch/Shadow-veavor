class_name HUD
extends CanvasLayer

signal pause_pressed
signal reset_pressed
signal codex_pressed
signal hint_pressed

@onready var title_label: Label = $TopBar/TitleLabel
@onready var hint_label: Label = $TopBar/HintLabel
@onready var resonance_bar: ProgressBar = $ResonanceContainer/ResonanceBar
@onready var resonance_label: Label = $ResonanceContainer/ResonanceLabel
@onready var pause_btn: Button = $TopBar/PauseButton
@onready var reset_btn: Button = $TopBar/ResetButton
@onready var codex_btn: Button = $TopBar/CodexButton

func _ready():
	pause_btn.pressed.connect(func(): pause_pressed.emit())
	reset_btn.pressed.connect(func(): reset_pressed.emit())
	codex_btn.pressed.connect(func(): codex_pressed.emit())

func update_level_info(level_num: int, level_title: String, level_hint: String, chapter_num: int = 1, chapter_title: String = ""):
	if chapter_title != "":
		title_label.text = "CH. %d • #%d: %s" % [chapter_num, level_num, level_title.to_upper()]
	else:
		title_label.text = "MISSION %d: %s" % [level_num, level_title.to_upper()]
	hint_label.text = level_hint

func update_resonance(pct: float):
	resonance_bar.value = pct
	resonance_label.text = "%d%%" % int(round(pct))
	if pct >= 85.0:
		resonance_label.modulate = Color(1.0, 0.88, 0.45, 1.0)
	else:
		resonance_label.modulate = Color(0.85, 0.85, 0.85, 1.0)
