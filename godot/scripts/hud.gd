class_name HUD
extends CanvasLayer

signal pause_pressed
signal reset_pressed
signal hint_pressed

@onready var title_label: Label = $TopBar/TitleLabel
@onready var hint_label: Label = $TopBar/HintLabel
@onready var resonance_bar: ProgressBar = $ResonanceContainer/ResonanceBar
@onready var resonance_label: Label = $ResonanceContainer/ResonanceLabel
@onready var pause_btn: Button = $TopBar/PauseButton
@onready var reset_btn: Button = $TopBar/ResetButton

func _ready():
	pause_btn.pressed.connect(func(): pause_pressed.emit())
	reset_btn.pressed.connect(func(): reset_pressed.emit())

func update_level_info(level_num: int, level_title: String, level_hint: String):
	title_label.text = "LEVEL %d: %s" % [level_num, level_title.to_upper()]
	hint_label.text = level_hint

func update_resonance(pct: float):
	resonance_bar.value = pct
	resonance_label.text = "%d%%" % int(round(pct))
	if pct >= 85.0:
		resonance_label.modulate = Color(1.0, 0.88, 0.45, 1.0)
	else:
		resonance_label.modulate = Color(0.85, 0.85, 0.85, 1.0)
