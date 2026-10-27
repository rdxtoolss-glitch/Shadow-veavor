class_name LevelCompleteModal
extends Control

signal next_pressed
signal replay_pressed

@onready var card: Panel = $Card
@onready var stars_container: HBoxContainer = $Card/StarsContainer
@onready var next_btn: Button = $Card/NextButton
@onready var replay_btn: Button = $Card/ReplayButton
@onready var stats_label: Label = $Card/StatsLabel

func _ready():
	hide()
	next_btn.pressed.connect(func(): next_pressed.emit())
	replay_btn.pressed.connect(func(): replay_pressed.emit())

func show_completion(level_num: int, time_sec: float, stars: int, audio_mgr: AudioManager = null):
	show()
	stats_label.text = "Crafted in %.1fs" % time_sec

	# Setup stars
	for i in range(3):
		var star = stars_container.get_child(i) as Label
		star.text = "★" if i < stars else "☆"
		star.modulate = Color(1.0, 0.85, 0.35, 1.0) if i < stars else Color(0.4, 0.35, 0.3, 0.6)
		star.scale = Vector2(0.5, 0.5)

	# Emil Kowalski modal entrance: start scale 0.95, cubic ease out, gentle slide
	card.scale = Vector2(0.95, 0.95)
	card.position.y = 480.0 + 36.0
	card.modulate.a = 0.0

	var tw = create_tween().set_parallel(true)
	tw.tween_property(card, "scale", Vector2(1.0, 1.0), 0.28).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_OUT)
	tw.tween_property(card, "position:y", 480.0, 0.28).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_OUT)
	tw.tween_property(card, "modulate:a", 1.0, 0.24)

	# Staggered star animation with crystalline bell chimes
	for i in range(stars):
		var star = stars_container.get_child(i) as Label
		var star_tw = create_tween()
		star_tw.tween_interval(0.24 + i * 0.18)
		star_tw.tween_callback(func():
			if audio_mgr: audio_mgr.play_star(i)
		)
		star_tw.tween_property(star, "scale", Vector2(1.25, 1.25), 0.18).set_trans(Tween.TRANS_BACK).set_ease(Tween.EASE_OUT)
		star_tw.tween_property(star, "scale", Vector2(1.0, 1.0), 0.10)
