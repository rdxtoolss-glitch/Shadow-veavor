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
	# Warm wooden clack (pitch ~420Hz decaying fast)
	_synthesize_wood_tap(420.0, 0.045, 0.35)

func play_drop():
	if not sound_enabled: return
	# Deeper wooden settling tap (~320Hz)
	_synthesize_wood_tap(320.0, 0.055, 0.4)

func play_detent():
	if not sound_enabled: return
	# Tiny soft click (~680Hz)
	_synthesize_wood_tap(680.0, 0.02, 0.18)

func play_resonance_chime(tier: int):
	if not sound_enabled: return
	# Harmonic glass bells
	var freq: float = 523.25 # C5
	if tier == 1: freq = 659.25 # E5
	elif tier == 2: freq = 783.99 # G5
	elif tier >= 3: freq = 1046.50 # C6
	_synthesize_bell(freq, 0.45, 0.4)

func _synthesize_wood_tap(freq: float, duration: float, volume: float):
	if not _playback: return
	var frames: int = int(duration * _sample_rate)
	var available: int = _playback.get_frames_available()
	var to_write: int = mini(frames, available)
	for i in range(to_write):
		var t: float = float(i) / _sample_rate
		var decay: float = exp(-t * 80.0)
		var sample: float = sin(2.0 * PI * freq * t) * decay * volume
		# Add a touch of noise burst for organic strike impact
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
		var decay: float = exp(-t * 6.5)
		# Dual harmonics (fundamental + 2.76x overtone)
		var sample: float = (sin(2.0 * PI * freq * t) * 0.75 + sin(2.0 * PI * freq * 2.76 * t) * 0.25) * decay * volume
		_playback.push_frame(Vector2(sample, sample))
