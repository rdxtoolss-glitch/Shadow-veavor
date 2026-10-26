class_name GameWorld3D
extends Node3D

@onready var camera: Camera3D = $Camera3D
@onready var candle_light: OmniLight3D = $CandleLight
@onready var fill_light: OmniLight3D = $FillLight
@onready var flame_mesh: MeshInstance3D = $Candle/Flame
@onready var dust_particles: CPUParticles3D = $DustParticles
@onready var tokens_root: Node3D = $TokensRoot

var _flicker_phase: float = 0.0
var _cam_base_pos: Vector3
var _target_cam_offset: Vector3 = Vector3.ZERO
var _cur_cam_offset: Vector3 = Vector3.ZERO

func _ready():
	_cam_base_pos = camera.position
	_setup_lighting()
	_setup_particles()

func _setup_lighting():
	candle_light.light_color = Color(1.0, 0.74, 0.38)
	candle_light.light_energy = 3.4
	candle_light.shadow_enabled = true
	candle_light.shadow_bias = 0.03
	candle_light.shadow_blur = 1.8

	fill_light.light_color = Color(0.18, 0.14, 0.22)
	fill_light.light_energy = 0.5
	fill_light.shadow_enabled = false

func _setup_particles():
	dust_particles.amount = 32
	dust_particles.lifetime = 6.0
	dust_particles.preprocess = 3.0
	dust_particles.emission_shape = CPUParticles3D.EMISSION_SHAPE_BOX
	dust_particles.emission_box_extents = Vector3(3.8, 1.8, 3.8)
	dust_particles.gravity = Vector3(0.0, 0.04, 0.0)
	dust_particles.initial_velocity_min = 0.02
	dust_particles.initial_velocity_max = 0.06
	dust_particles.color = Color(1.0, 0.85, 0.45, 0.32)

	var pmesh = SphereMesh.new()
	pmesh.radius = 0.015
	pmesh.height = 0.03
	dust_particles.mesh = pmesh

func _process(dt: float):
	# Dynamic multi-sine candle flicker
	_flicker_phase += dt * 4.8
	var f1: float = sin(_flicker_phase) * 0.12
	var f2: float = sin(_flicker_phase * 2.3) * 0.07
	var f3: float = sin(_flicker_phase * 5.7) * 0.04
	var flicker_total: float = f1 + f2 + f3

	candle_light.light_energy = 3.4 + flicker_total * 1.2
	flame_mesh.scale = Vector3(1.0 + f1 * 0.5, 1.0 + flicker_total * 0.8, 1.0 + f2 * 0.5)

	# Smooth camera parallax interpolation (Emil Kowalski spring relaxation)
	_cur_cam_offset = _cur_cam_offset.lerp(_target_cam_offset, minf(1.0, dt * 6.0))
	camera.position = _cam_base_pos + _cur_cam_offset
	camera.rotation.y = -_cur_cam_offset.x * 0.04
	camera.rotation.x = -0.45 - _cur_cam_offset.z * 0.03

func set_parallax_target(norm_screen_pos: Vector2):
	# norm_screen_pos in [-1, +1]
	_target_cam_offset = Vector3(norm_screen_pos.x * 0.35, norm_screen_pos.y * 0.15, norm_screen_pos.y * 0.25)
