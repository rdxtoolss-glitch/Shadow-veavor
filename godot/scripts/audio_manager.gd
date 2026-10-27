class_name AudioManager
extends Node

var sound_enabled: bool = true:
	set(val):
		sound_enabled = val
		_apply_sound_state()

var haptics_enabled: bool = true

# Audio Streams
var _picks: Array[AudioStream] = []
var _drops: Array[AudioStream] = []
var _gear_ticks: Array[AudioStream] = []
var _detent: AudioStream
var _chimes: Array[AudioStream] = []
var _victory: AudioStream
var _slide_stream: AudioStream
var _resonance_stream: AudioStream
var _ambience_stream: AudioStream
var _parchment_open: AudioStream
var _parchment_flip: AudioStream
var _parchment_close: AudioStream
var _ui_tap: AudioStream
var _stars: Array[AudioStream] = []

# Audio Players
var _ambience_player: AudioStreamPlayer
var _resonance_player: AudioStreamPlayer
var _slide_player: AudioStreamPlayer
var _music_player: AudioStreamPlayer
var _sfx_pool: Array[AudioStreamPlayer] = []
var _sfx_pool_idx: int = 0
const SFX_POOL_SIZE: int = 8

# Smooth Dynamic State
var _target_resonance_db: float = -60.0
var _current_resonance_db: float = -60.0
var _target_slide_db: float = -60.0
var _current_slide_db: float = -60.0
var _is_sliding: bool = false

func _ready():
	_load_streams()
	_setup_players()
	_apply_sound_state()

func _load_streams():
	_picks = [
		_load_stream("res://assets/audio/token_pick_01.wav"),
		_load_stream("res://assets/audio/token_pick_02.wav"),
		_load_stream("res://assets/audio/token_pick_03.wav")
	]
	_drops = [
		_load_stream("res://assets/audio/token_drop_01.wav"),
		_load_stream("res://assets/audio/token_drop_02.wav"),
		_load_stream("res://assets/audio/token_drop_03.wav")
	]
	_gear_ticks = [
		_load_stream("res://assets/audio/gear_tick_01.wav"),
		_load_stream("res://assets/audio/gear_tick_02.wav")
	]
	_detent = _load_stream("res://assets/audio/detent_snap.wav")
	_chimes = [
		_load_stream("res://assets/audio/chime_50.wav"),
		_load_stream("res://assets/audio/chime_75.wav"),
		_load_stream("res://assets/audio/chime_90.wav")
	]
	_victory = _load_stream("res://assets/audio/victory_flourish.wav")

	_slide_stream = _load_stream("res://assets/audio/token_slide_loop.wav", true)
	_resonance_stream = _load_stream("res://assets/audio/resonance_drone_loop.wav", true)
	_ambience_stream = _load_stream("res://assets/audio/ambience_study_loop.wav", true)

	_parchment_open = _load_stream("res://assets/audio/parchment_open.wav")
	_parchment_flip = _load_stream("res://assets/audio/parchment_flip.wav")
	_parchment_close = _load_stream("res://assets/audio/parchment_close.wav")
	_ui_tap = _load_stream("res://assets/audio/ui_tap.wav")

	_stars = [
		_load_stream("res://assets/audio/star_01.wav"),
		_load_stream("res://assets/audio/star_02.wav"),
		_load_stream("res://assets/audio/star_03.wav")
	]

func _load_stream(path: String, is_loop: bool = false) -> AudioStream:
	if not ResourceLoader.exists(path):
		return null
	var s = load(path) as AudioStream
	if is_loop and s is AudioStreamWAV:
		s.loop_mode = AudioStreamWAV.LOOP_FORWARD
	return s

func _setup_players():
	# 1. Ambience player (candlelight study bed)
	_ambience_player = AudioStreamPlayer.new()
	_ambience_player.stream = _ambience_stream
	_ambience_player.volume_db = -18.0
	add_child(_ambience_player)
	if sound_enabled and _ambience_stream:
		_ambience_player.play()

	# 2. Resonance drone player (dynamic singing bowl alignment bed)
	_resonance_player = AudioStreamPlayer.new()
	_resonance_player.stream = _resonance_stream
	_resonance_player.volume_db = -60.0
	add_child(_resonance_player)
	if sound_enabled and _resonance_stream:
		_resonance_player.play()

	# 3. Slide friction player (velocity-controlled table friction)
	_slide_player = AudioStreamPlayer.new()
	_slide_player.stream = _slide_stream
	_slide_player.volume_db = -60.0
	add_child(_slide_player)

	# 4. Music / Victory fanfare player
	_music_player = AudioStreamPlayer.new()
	_music_player.volume_db = -4.0
	add_child(_music_player)

	# 5. Polyphonic SFX Pool
	_sfx_pool.clear()
	for i in range(SFX_POOL_SIZE):
		var p = AudioStreamPlayer.new()
		p.volume_db = -2.0
		add_child(p)
		_sfx_pool.append(p)

func _process(dt: float):
	if not sound_enabled: return

	# Smooth exponential smoothing on dynamic audio channels
	# 1. Resonance alignment drone
	_current_resonance_db = lerpf(_current_resonance_db, _target_resonance_db, dt * 6.0)
	if _resonance_player:
		_resonance_player.volume_db = _current_resonance_db

	# 2. Wood slide friction
	_current_slide_db = lerpf(_current_slide_db, _target_slide_db, dt * 14.0)
	if _slide_player:
		_slide_player.volume_db = _current_slide_db
		if not _is_sliding and _current_slide_db < -52.0 and _slide_player.playing:
			_slide_player.stop()

func _apply_sound_state():
	if not is_inside_tree(): return

	if not sound_enabled:
		if _ambience_player: _ambience_player.stop()
		if _resonance_player: _resonance_player.stop()
		if _slide_player: _slide_player.stop()
		if _music_player: _music_player.stop()
		for p in _sfx_pool: p.stop()
	else:
		if _ambience_player and _ambience_stream and not _ambience_player.playing:
			_ambience_player.play()
		if _resonance_player and _resonance_stream and not _resonance_player.playing:
			_resonance_player.play()

# -------------------------------------------------------------
# GAMEPLAY SOUND API
# -------------------------------------------------------------

func play_pick():
	if not sound_enabled or _picks.is_empty(): return
	var stream = _picks.pick_random()
	_play_sfx(stream, randf_range(0.96, 1.04), -1.0)
	_trigger_haptic(15)

func play_drop():
	if not sound_enabled or _drops.is_empty(): return
	var stream = _drops.pick_random()
	_play_sfx(stream, randf_range(0.95, 1.03), 0.0)
	_trigger_haptic(25)

func start_slide():
	if not sound_enabled or not _slide_stream: return
	_is_sliding = true
	_target_slide_db = -38.0
	if not _slide_player.playing:
		_slide_player.play()

func update_slide(speed: float):
	if not sound_enabled or not _is_sliding: return
	# Map drag speed (0 - 800 scene px/s) to volume (-38 dB to -12 dB) and subtle pitch
	var norm_speed = clampf(speed / 750.0, 0.0, 1.0)
	_target_slide_db = lerpf(-38.0, -12.0, norm_speed)
	_slide_player.pitch_scale = lerpf(0.94, 1.08, norm_speed)

func stop_slide():
	_is_sliding = false
	_target_slide_db = -60.0

func play_gear_tick():
	if not sound_enabled or _gear_ticks.is_empty(): return
	var stream = _gear_ticks.pick_random()
	_play_sfx(stream, randf_range(0.97, 1.05), -4.0)
	_trigger_haptic(8)

func play_detent():
	if not sound_enabled or not _detent: return
	_play_sfx(_detent, randf_range(0.98, 1.02), -1.0)
	_trigger_haptic(18)

func update_resonance_alignment(iou: float):
	if not sound_enabled: return
	# Below 20% alignment: silent drone
	# 20% to 100%: warm swell from -34 dB up to -8 dB
	if iou < 0.20:
		_target_resonance_db = -60.0
	else:
		var norm = clampf((iou - 0.20) / 0.80, 0.0, 1.0)
		_target_resonance_db = lerpf(-34.0, -8.0, pow(norm, 1.2))
		if _resonance_player:
			_resonance_player.pitch_scale = lerpf(0.98, 1.03, norm)

func play_resonance_chime(tier: int):
	if not sound_enabled or _chimes.is_empty(): return
	var idx = clampi(tier - 1, 0, _chimes.size() - 1)
	_play_sfx(_chimes[idx], 1.0, 1.0)
	_trigger_haptic(35)

func play_victory():
	if not sound_enabled: return
	if _victory and _music_player:
		_music_player.stream = _victory
		_music_player.pitch_scale = 1.0
		_music_player.play()

	# Swell resonance drone to golden climax, then gently relax
	_target_resonance_db = -4.0
	var tw = create_tween()
	tw.tween_interval(1.2)
	tw.tween_callback(func(): _target_resonance_db = -18.0)
	_trigger_haptic(50)

# -------------------------------------------------------------
# UI & PARCHMENT API
# -------------------------------------------------------------

func play_parchment_open():
	if not sound_enabled or not _parchment_open: return
	_play_sfx(_parchment_open, randf_range(0.97, 1.03), 0.0)

func play_parchment_flip():
	if not sound_enabled or not _parchment_flip: return
	_play_sfx(_parchment_flip, randf_range(0.96, 1.04), -1.0)

func play_parchment_close():
	if not sound_enabled or not _parchment_close: return
	_play_sfx(_parchment_close, 1.0, 0.0)

func play_ui_tap():
	if not sound_enabled or not _ui_tap: return
	_play_sfx(_ui_tap, randf_range(0.96, 1.04), -2.0)
	_trigger_haptic(10)

func play_star(star_idx: int):
	if not sound_enabled or _stars.is_empty(): return
	var idx = clampi(star_idx, 0, _stars.size() - 1)
	_play_sfx(_stars[idx], 1.0, 1.5)
	_trigger_haptic(20)

# -------------------------------------------------------------
# INTERNAL HELPERS
# -------------------------------------------------------------

func _play_sfx(stream: AudioStream, pitch: float = 1.0, vol_db: float = 0.0):
	if not sound_enabled or stream == null: return
	var player = _sfx_pool[_sfx_pool_idx]
	_sfx_pool_idx = (_sfx_pool_idx + 1) % SFX_POOL_SIZE

	player.stream = stream
	player.pitch_scale = pitch
	player.volume_db = vol_db
	player.play()

func _trigger_haptic(duration_ms: int):
	if not haptics_enabled: return
	if OS.has_feature("mobile"):
		Input.vibrate_handheld(duration_ms)
