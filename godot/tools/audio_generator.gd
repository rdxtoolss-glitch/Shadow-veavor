extends SceneTree

const SAMPLE_RATE: int = 44100
const AUDIO_DIR: String = "res://assets/audio/"

func _init():
	print("==================================================")
	print("GENERATING ACOUSTIC SOUND EFFECTS FOR SHADOWWEAVER")
	print("==================================================")

	var dir = DirAccess.open("res://")
	if not dir.dir_exists("assets/audio"):
		dir.make_dir_recursive("assets/audio")

	# 1. Tactile Wood Tokens
	_generate_wood_pick("token_pick_01.wav", 410.0, 0.08)
	_generate_wood_pick("token_pick_02.wav", 445.0, 0.075)
	_generate_wood_pick("token_pick_03.wav", 380.0, 0.085)

	_generate_wood_drop("token_drop_01.wav", 200.0, 0.16)
	_generate_wood_drop("token_drop_02.wav", 225.0, 0.15)
	_generate_wood_drop("token_drop_03.wav", 185.0, 0.18)

	_generate_wood_slide("token_slide_loop.wav", 1.5)

	# 2. Clockwork & Brass Astrolabe
	_generate_gear_tick("gear_tick_01.wav", 2900.0, 0.035)
	_generate_gear_tick("gear_tick_02.wav", 3300.0, 0.032)
	_generate_detent_snap("detent_snap.wav", 1850.0, 0.09)

	# 3. Dynamic Resonance & Celestial Bells
	_generate_resonance_drone("resonance_drone_loop.wav", 3.0)
	_generate_bell_chime("chime_50.wav", [523.25], 0.85)
	_generate_bell_chime("chime_75.wav", [659.25, 783.99], 0.95)
	_generate_bell_chime("chime_90.wav", [523.25, 783.99, 1046.50], 1.1)
	_generate_victory_flourish("victory_flourish.wav", 1.8)

	# 4. Parchment & UI
	_generate_parchment_rustle("parchment_open.wav", 0.28, 0.7)
	_generate_parchment_rustle("parchment_flip.wav", 0.20, 0.9)
	_generate_parchment_close("parchment_close.wav", 0.26)
	_generate_ui_tap("ui_tap.wav", 640.0, 0.03)

	_generate_bell_chime("star_01.wav", [880.0], 0.65)
	_generate_bell_chime("star_02.wav", [1108.73], 0.70)
	_generate_bell_chime("star_03.wav", [1318.51, 1661.22], 0.85)

	# 5. Ambient Study Tone
	_generate_study_ambience("ambience_study_loop.wav", 4.0)

	print("--------------------------------------------------")
	print("ALL SOUND EFFECTS GENERATED & SAVED TO ASSETS/AUDIO!")
	print("--------------------------------------------------")
	quit(0)

# -------------------------------------------------------------
# WAV FILE WRITER HELPER
# -------------------------------------------------------------
func _save_wav_file(filename: String, samples: Array[float], is_looping: bool = false, loop_start: int = 0, loop_end: int = 0):
	var path = AUDIO_DIR + filename
	var file = FileAccess.open(path, FileAccess.WRITE)
	if not file:
		printerr("Failed to open file for writing: ", path)
		return

	var num_samples = samples.size()
	var byte_rate = SAMPLE_RATE * 2 # 16-bit mono = 2 bytes per sample
	var block_align = 2
	var data_size = num_samples * 2
	var chunk_size = 36 + data_size

	# RIFF header
	file.store_string("RIFF")
	file.store_32(chunk_size)
	file.store_string("WAVE")

	# fmt subchunk
	file.store_string("fmt ")
	file.store_32(16) # Subchunk1Size (16 for PCM)
	file.store_16(1)  # AudioFormat (1 for PCM)
	file.store_16(1)  # NumChannels (1 mono)
	file.store_32(SAMPLE_RATE)
	file.store_32(byte_rate)
	file.store_16(block_align)
	file.store_16(16) # BitsPerSample

	# data subchunk
	file.store_string("data")
	file.store_32(data_size)

	# Sample buffer (16-bit signed PCM)
	for s in samples:
		var clamped = clampf(s, -1.0, 1.0)
		var val16 = int(round(clamped * 32767.0))
		file.store_16(val16)

	file.close()
	print("  -> Saved %-26s (%5.2fs, %6d samples)" % [filename, float(num_samples) / SAMPLE_RATE, num_samples])

# -------------------------------------------------------------
# ACOUSTIC SYNTHESIZERS
# -------------------------------------------------------------

# 1. Tactile Wood Pick: Initial grip scrape + hollow timber cavity resonances
func _generate_wood_pick(filename: String, freq: float, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE
		var decay = exp(-t * 85.0)

		# Modal timber modes
		var mode1 = sin(2.0 * PI * freq * t) * 0.65
		var mode2 = sin(2.0 * PI * freq * 2.14 * t) * 0.25
		var mode3 = sin(2.0 * PI * freq * 3.28 * t) * 0.10
		var tone = (mode1 + mode2 + mode3) * decay

		# Tactile finger skin scrape transient
		var scrape = 0.0
		if t < 0.012:
			var scrape_env = (0.012 - t) / 0.012
			var noise = (randf() * 2.0 - 1.0)
			scrape = noise * scrape_env * 0.45

		samples[i] = (tone * 0.75 + scrape) * 0.85

	_save_wav_file(filename, samples)

# 2. Wood Drop: Impact transient + body cavity + micro bounce
func _generate_wood_drop(filename: String, freq: float, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	var bounce_time = 0.032 # Secondary micro settle bounce

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE

		# Primary hit
		var decay1 = exp(-t * 55.0)
		var body1 = sin(2.0 * PI * freq * t) * 0.70 + sin(2.0 * PI * freq * 1.95 * t) * 0.30
		var hit1 = body1 * decay1

		# Secondary micro bounce
		var hit2 = 0.0
		if t >= bounce_time:
			var t2 = t - bounce_time
			var decay2 = exp(-t2 * 80.0)
			hit2 = sin(2.0 * PI * freq * 1.15 * t2) * decay2 * 0.28

		# Initial impact click
		var click = 0.0
		if t < 0.006:
			var click_env = (0.006 - t) / 0.006
			click = (randf() * 2.0 - 1.0) * click_env * 0.55

		samples[i] = (hit1 * 0.65 + hit2 + click) * 0.88

	_save_wav_file(filename, samples)

# 3. Wood Slide Friction Loop: Grainy wood/felt friction
func _generate_wood_slide(filename: String, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	var b0 = 0.0
	var b1 = 0.0
	var b2 = 0.0

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE
		var white = randf() * 2.0 - 1.0

		# 3-pole resonant bandpass filter around wood friction frequencies (520 Hz)
		b0 = 0.95 * b0 + white * 0.05
		b1 = 0.92 * b1 + b0 * 0.08
		b2 = 0.88 * b2 + b1 * 0.12

		# Formant body resonance
		var formant = sin(2.0 * PI * 480.0 * t) * 0.12 + sin(2.0 * PI * 820.0 * t) * 0.06
		var raw = (b0 * 0.5 + b1 * 0.3 + b2 * 0.2 + formant) * 0.75

		# Micro wood grain modulation
		var grain = (sin(2.0 * PI * 34.0 * t) * 0.15 + 0.85)
		samples[i] = raw * grain * 0.55

	# Loop crossfade boundary (first/last 2000 samples)
	var fade_len = 2000
	for i in range(fade_len):
		var w = float(i) / float(fade_len)
		var end_idx = num_samples - fade_len + i
		var blended = samples[i] * w + samples[end_idx] * (1.0 - w)
		samples[i] = blended
		samples[end_idx] = blended

	_save_wav_file(filename, samples, true, 0, num_samples)

# 4. Precision Swiss Brass Escapement Gear Tick
func _generate_gear_tick(filename: String, freq: float, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE
		var decay = exp(-t * 220.0)

		# Metallic high overtones
		var ring = (sin(2.0 * PI * freq * t) * 0.6 + sin(2.0 * PI * freq * 1.58 * t) * 0.4) * decay

		# Spring catch click
		var click = 0.0
		if t < 0.003:
			click = (randf() * 2.0 - 1.0) * (0.003 - t) / 0.003 * 0.8

		samples[i] = (ring * 0.6 + click * 0.4) * 0.8

	_save_wav_file(filename, samples)

# 5. 45° Magnetic Detent Snap: Double latch click + brass resonance
func _generate_detent_snap(filename: String, freq: float, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	var second_click_time = 0.011

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE

		# First latch strike
		var decay1 = exp(-t * 110.0)
		var ring1 = sin(2.0 * PI * freq * t) * decay1

		# Second seat snap
		var ring2 = 0.0
		if t >= second_click_time:
			var t2 = t - second_click_time
			var decay2 = exp(-t2 * 140.0)
			ring2 = sin(2.0 * PI * freq * 1.22 * t2) * decay2 * 0.85

		var click = 0.0
		if t < 0.004:
			click = (randf() * 2.0 - 1.0) * 0.6
		if t >= second_click_time and t < second_click_time + 0.004:
			click += (randf() * 2.0 - 1.0) * 0.5

		samples[i] = (ring1 * 0.45 + ring2 * 0.45 + click * 0.3) * 0.85

	_save_wav_file(filename, samples)

# 6. Ethereal Glass Armonica / Tibetan Singing Bowl Drone Loop
func _generate_resonance_drone(filename: String, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	# Frequencies: C4 (261.63), G4 (392.00), C5 (523.25), E5 (659.25), B5 (987.77)
	var freqs = [261.63, 392.00, 523.25, 659.25, 987.77]
	var weights = [0.45, 0.30, 0.22, 0.14, 0.07]

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE
		var lfo = sin(2.0 * PI * 0.35 * t) * 0.12 + 0.88 # 0.35Hz subtle breathing shimmer

		var val = 0.0
		for f_idx in range(freqs.size()):
			var f = freqs[f_idx]
			var w = weights[f_idx]
			# Slight micro-detune chorus for rich acoustic body
			var s1 = sin(2.0 * PI * f * t)
			var s2 = sin(2.0 * PI * (f + 0.4) * t)
			val += (s1 + s2 * 0.5) * w

		samples[i] = val * lfo * 0.55

	# Loop crossfade boundary
	var fade_len = 3500
	for i in range(fade_len):
		var w = float(i) / float(fade_len)
		var end_idx = num_samples - fade_len + i
		var blended = samples[i] * w + samples[end_idx] * (1.0 - w)
		samples[i] = blended
		samples[end_idx] = blended

	_save_wav_file(filename, samples, true, 0, num_samples)

# 7. Harmonic Crystalline Bell Chimes
func _generate_bell_chime(filename: String, freqs: Array, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE
		var decay = exp(-t * 5.2)

		var chime = 0.0
		for f in freqs:
			var fundamental = sin(2.0 * PI * f * t) * 0.70
			var overtone = sin(2.0 * PI * f * 2.756 * t) * 0.24
			var crystal = sin(2.0 * PI * f * 5.404 * t) * 0.06
			chime += (fundamental + overtone + crystal)

		chime /= float(freqs.size())

		# Tiny strike impact
		var strike = 0.0
		if t < 0.005:
			strike = (randf() * 2.0 - 1.0) * (0.005 - t) / 0.005 * 0.35

		samples[i] = (chime * decay * 0.85 + strike * 0.25) * 0.88

	_save_wav_file(filename, samples)

# 8. Transcendent Victory Flourish: Seal Stamp Thud + Ascending Chime Arpeggio
func _generate_victory_flourish(filename: String, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	# Arpeggio notes: C5 (0.0s), E5 (0.07s), G5 (0.14s), C6 (0.21s), E6 (0.28s)
	var notes = [523.25, 659.25, 783.99, 1046.50, 1318.51]
	var note_times = [0.0, 0.07, 0.14, 0.21, 0.28]

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE

		# Rich low wax seal stamp thud at t=0
		var thud_decay = exp(-t * 35.0)
		var thud = (sin(2.0 * PI * 110.0 * t) * 0.6 + sin(2.0 * PI * 65.0 * t) * 0.4) * thud_decay * 0.65

		# Chime arpeggio
		var chime_sum = 0.0
		for n_idx in range(notes.size()):
			var start_t = note_times[n_idx]
			if t >= start_t:
				var note_t = t - start_t
				var decay = exp(-note_t * 4.2)
				var f = notes[n_idx]
				var s = (sin(2.0 * PI * f * note_t) * 0.75 + sin(2.0 * PI * f * 2.76 * note_t) * 0.25) * decay
				chime_sum += s * 0.35

		samples[i] = (thud * 0.5 + chime_sum * 0.7) * 0.85

	_save_wav_file(filename, samples)

# 9. Antique Parchment Vellum Rustle
func _generate_parchment_rustle(filename: String, duration: float, crispness: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	var lp = 0.0
	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE
		# Parabolic swell and decay envelope
		var norm_t = t / duration
		var env = sin(PI * norm_t)
		env = pow(env, 1.4)

		var white = randf() * 2.0 - 1.0
		# One-pole filter for paper friction
		lp = lp * 0.78 + white * 0.22

		# High frequency paper crinkles
		var crinkle = 0.0
		if randf() < 0.08:
			crinkle = (randf() * 2.0 - 1.0) * crispness * 0.6

		samples[i] = (lp * 0.7 + crinkle) * env * 0.65

	_save_wav_file(filename, samples)

# 10. Parchment / Leather Book Close Thud
func _generate_parchment_close(filename: String, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE
		var decay = exp(-t * 45.0)
		var thud = sin(2.0 * PI * 140.0 * t) * decay * 0.65
		var slap = (randf() * 2.0 - 1.0) * exp(-t * 120.0) * 0.45
		samples[i] = (thud + slap) * 0.75

	_save_wav_file(filename, samples)

# 11. UI Wooden Tap
func _generate_ui_tap(filename: String, freq: float, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE
		var decay = exp(-t * 180.0)
		var tone = sin(2.0 * PI * freq * t) * decay * 0.7
		var click = (randf() * 2.0 - 1.0) * exp(-t * 300.0) * 0.4
		samples[i] = (tone + click) * 0.75

	_save_wav_file(filename, samples)

# 12. Ambient Study Tone: Low room warmth + soft candle flutter
func _generate_study_ambience(filename: String, duration: float):
	var num_samples = int(duration * SAMPLE_RATE)
	var samples: Array[float] = []
	samples.resize(num_samples)

	var lp = 0.0
	for i in range(num_samples):
		var t = float(i) / SAMPLE_RATE

		# Deep warm room cushion (55 Hz + 110 Hz drone, very quiet)
		var room = (sin(2.0 * PI * 55.0 * t) * 0.035 + sin(2.0 * PI * 110.0 * t) * 0.015)

		# Soft candle flutter (filtered pink noise)
		var white = randf() * 2.0 - 1.0
		lp = lp * 0.96 + white * 0.04
		var flutter = lp * 0.08

		# Distant pendulum tick every 1.0s (muffled)
		var tick = 0.0
		var sec_frac = fposmod(t, 1.0)
		if sec_frac < 0.035:
			tick = sin(2.0 * PI * 220.0 * sec_frac) * exp(-sec_frac * 90.0) * 0.04

		samples[i] = (room + flutter + tick) * 0.65

	# Loop crossfade boundary
	var fade_len = 4000
	for i in range(fade_len):
		var w = float(i) / float(fade_len)
		var end_idx = num_samples - fade_len + i
		var blended = samples[i] * w + samples[end_idx] * (1.0 - w)
		samples[i] = blended
		samples[end_idx] = blended

	_save_wav_file(filename, samples, true, 0, num_samples)
