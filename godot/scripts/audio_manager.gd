class_name AudioManager
extends Node

var _player: AudioStreamPlayer
var _generator: AudioStreamGenerator
var _playback: AudioStreamGeneratorPlayback
var _sample_rate: float = 44100.0

var sound_enabled: bool = true
var haptics_enabled: bool = true

func _ready():
	_player = AudioStreamPlayer.new()
	add_child(_player)
	_generator = AudioStreamGenerator.new()
	_generator.mix_rate = _sample_rate
	_generator.buffer_length = 0.1
	_player.stream = _generator
	_player.play()
	_playback = _player.get_stream_playback() as AudioStreamGeneratorPlayback

func play_pick():
	if not sound_enabled: return
	# Warm wooden clack with subtle organic pitch variation (395-445 Hz)
	var freq: float = randf_range(395.0, 445.0)
	_synthesize_wood_tap(freq, 0.045, 0.35)
	_trigger_haptic(15)

func play_drop():
	if not sound_enabled: return
	# Deeper wooden settling tap with subtle variation (305-335 Hz)
	var freq: float = randf_range(305.0, 335.0)
	_synthesize_wood_tap(freq, 0.055, 0.40)
	_trigger_haptic(25)

func play_detent():
	if not sound_enabled: return
	# Tiny soft mechanical click (660-710 Hz)
	var freq: float = randf_range(660.0, 710.0)
	_synthesize_wood_tap(freq, 0.020, 0.18)
	_trigger_haptic(10)

func play_resonance_chime(tier: int):
	if not sound_enabled: return
	# Harmonic crystalline bells
	var freq: float = 523.25 # C5
	if tier == 1: freq = 659.25 # E5
	elif tier == 2: freq = 783.99 # G5
	elif tier >= 3: freq = 1046.50 # C6
	_synthesize_bell(freq, 0.50, 0.42)
	_trigger_haptic(40)

func _trigger_haptic(duration_ms: int):
	if not haptics_enabled: return
	if OS.has_feature("mobile"):
		Input.vibrate_handheld(duration_ms)

func _synthesize_wood_tap(freq: float, duration: float, volume: float):
	if not _playback: return
	var frames: int = int(duration * _sample_rate)
	var available: int = _playback.get_frames_available()
	var to_write: int = mini(frames, available)
	for i in range(to_write):
		var t: float = float(i) / _sample_rate
		var decay: float = exp(-t * 85.0)
		var sample: float = sin(2.0 * PI * freq * t) * decay * volume
		# Micro noise transient for organic physical timber impact
		if t < 0.005:
			sample += (randf() * 2.0 - 1.0) * (0.005 - t) * 120.0 * volume
		_playback.push_frame(Vector2(sample, sample))

func _synthesize_bell(freq: float, duration: float, volume: float):
	if not _playback: return
	var frames: int = int(duration * _sample_rate)
	var available: int = _playback.get_frames_available()
	var to_write: int = mini(frames, available)
	for i in range(to_write):
		var t: float = float(i) / _sample_rate
		var decay: float = exp(-t * 6.0)
		# Dual harmonics (fundamental + 2.76x crystalline overtone)
		var sample: float = (sin(2.0 * PI * freq * t) * 0.72 + sin(2.0 * PI * freq * 2.76 * t) * 0.28) * decay * volume
		_playback.push_frame(Vector2(sample, sample))
