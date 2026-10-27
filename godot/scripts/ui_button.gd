class_name UiButton
extends Button

@export var primary_color: Color = Color(0.85, 0.65, 0.28, 1.0)
@export var is_pill: bool = true

var _target_scale: float = 1.0
var _current_scale: float = 1.0
var _scale_velocity: float = 0.0

func _ready():
	pivot_offset = size * 0.5
	button_down.connect(_on_down)
	button_up.connect(_on_up)
	resized.connect(_on_resized)

func _on_resized():
	pivot_offset = size * 0.5

func _on_down():
	_target_scale = 0.96
	if get_tree() and get_tree().root:
		var audio_mgr = get_tree().root.find_child("AudioManager", true, false)
		if audio_mgr and audio_mgr.has_method("play_ui_tap"):
			audio_mgr.play_ui_tap()

func _on_up():
	_target_scale = 1.0

func _process(dt: float):
	# Spring dynamics: f = -k * x - c * v
	var k: float = 340.0
	var c: float = 26.0
	var displacement: float = _target_scale - _current_scale
	var force: float = k * displacement - c * _scale_velocity
	_scale_velocity += force * dt
	_current_scale += _scale_velocity * dt
	scale = Vector2(_current_scale, _current_scale)
