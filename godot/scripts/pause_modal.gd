class_name PauseModal
extends Control

signal resume_pressed
signal restart_pressed
signal sound_toggled(enabled: bool)

@onready var card: Panel = $Card
@onready var resume_btn: Button = $Card/VBox/ResumeButton
@onready var restart_btn: Button = $Card/VBox/RestartButton
@onready var sound_btn: Button = $Card/VBox/SoundButton

var _sound_on: bool = true

func _ready():
	hide()
	resume_btn.pressed.connect(func():
		hide()
		resume_pressed.emit()
	)
	restart_btn.pressed.connect(func():
		hide()
		restart_pressed.emit()
	)
	sound_btn.pressed.connect(func():
		_sound_on = not _sound_on
		_update_sound_label()
		sound_toggled.emit(_sound_on)
	)

func open():
	show()
	card.scale = Vector2(0.95, 0.95)
	card.position.y = 480.0 + 30.0
	card.modulate.a = 0.0

	var tw = create_tween().set_parallel(true)
	tw.tween_property(card, "scale", Vector2(1.0, 1.0), 0.24).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_OUT)
	tw.tween_property(card, "position:y", 480.0, 0.24).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_OUT)
	tw.tween_property(card, "modulate:a", 1.0, 0.20)

func _update_sound_label():
	sound_btn.text = "SOUND: ON" if _sound_on else "SOUND: OFF"
