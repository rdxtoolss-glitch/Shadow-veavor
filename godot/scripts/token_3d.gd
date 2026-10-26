class_name Token3D
extends Node3D

signal picked(token: Token3D)
signal dragged(token: Token3D)
signal released(token: Token3D)
signal rotated(token: Token3D)

var shape_data: ShapeObject
var is_selected: bool = false

var _body_mesh: MeshInstance3D
var _rim_mesh: MeshInstance3D
var _rivet_mesh: MeshInstance3D
var _halo_mesh: MeshInstance3D
var _handle_mesh: MeshInstance3D
var _shadow_decal: Decal

var _drag_plane: Plane = Plane(Vector3.UP, 0.0)
var _drag_offset_3d: Vector3 = Vector3.ZERO
var _is_handle_dragging: bool = false
var _initial_rot_angle: float = 0.0
var _initial_shape_rot: float = 0.0

# Materials
static var _mat_wood: StandardMaterial3D
static var _mat_brass: StandardMaterial3D
static var _mat_halo: StandardMaterial3D
static var _mat_handle: StandardMaterial3D

const SCALE_FACTOR: float = 0.01 # 100 scene units = 1.0 3D meter

static func _init_materials():
	if _mat_wood != null: return

	# Deep rich antique walnut
	_mat_wood = StandardMaterial3D.new()
	_mat_wood.albedo_color = Color(0.24, 0.16, 0.11, 1.0)
	_mat_wood.roughness = 0.32
	_mat_wood.metallic = 0.08
	_mat_wood.metallic_specular = 0.6

	# Polished artisan brass
	_mat_brass = StandardMaterial3D.new()
	_mat_brass.albedo_color = Color(0.88, 0.72, 0.36, 1.0)
	_mat_brass.metallic = 0.88
	_mat_brass.roughness = 0.24
	_mat_brass.clearcoat = 0.4
	_mat_brass.clearcoat_roughness = 0.2

	# Luminous selection halo
	_mat_halo = StandardMaterial3D.new()
	_mat_halo.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	_mat_halo.albedo_color = Color(1.0, 0.85, 0.45, 0.75)
	_mat_halo.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA

	_mat_handle = StandardMaterial3D.new()
	_mat_handle.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	_mat_handle.albedo_color = Color(1.0, 0.95, 0.8, 1.0)

func setup(data: ShapeObject):
	shape_data = data
	_init_materials()
	_build_3d_meshes()
	update_from_data()

func _build_3d_meshes():
	# Clear old children
	for c in get_children():
		c.queue_free()

	_body_mesh = MeshInstance3D.new()
	_rim_mesh = MeshInstance3D.new()
	_rivet_mesh = MeshInstance3D.new()
	_halo_mesh = MeshInstance3D.new()
	_handle_mesh = MeshInstance3D.new()

	var w: float = shape_data.width * SCALE_FACTOR
	var h: float = shape_data.height * SCALE_FACTOR
	var thickness: float = 0.16

	match shape_data.kind:
		ShapeObject.Kind.CIRCLE:
			var r: float = w * 0.5
			var cyl = CylinderMesh.new()
			cyl.top_radius = r
			cyl.bottom_radius = r
			cyl.height = thickness
			cyl.radial_segments = 36
			_body_mesh.mesh = cyl
			_body_mesh.material_override = _mat_wood

			var rim = TorusMesh.new()
			rim.inner_radius = r - 0.02
			rim.outer_radius = r + 0.02
			rim.rings = 24
			rim.ring_segments = 12
			_rim_mesh.mesh = rim
			_rim_mesh.material_override = _mat_brass
			_rim_mesh.position.y = thickness * 0.4

		ShapeObject.Kind.TRIANGLE:
			var prism = PrismMesh.new()
			prism.size = Vector3(w, thickness, h)
			prism.left_to_right = 0.5
			_body_mesh.mesh = prism
			_body_mesh.material_override = _mat_wood
			# Rotate so apex points forward along Z
			_body_mesh.rotation_degrees.x = -90.0

		ShapeObject.Kind.SQUARE, ShapeObject.Kind.RECT, ShapeObject.Kind.LINE:
			var box = BoxMesh.new()
			box.size = Vector3(w, thickness, h)
			_body_mesh.mesh = box
			_body_mesh.material_override = _mat_wood

			var rim_box = BoxMesh.new()
			rim_box.size = Vector3(w + 0.03, thickness * 0.6, h + 0.03)
			_rim_mesh.mesh = rim_box
			_rim_mesh.material_override = _mat_brass

	# Center artisan brass rivet
	var rivet = SphereMesh.new()
	rivet.radius = 0.04
	rivet.height = 0.08
	_rivet_mesh.mesh = rivet
	_rivet_mesh.material_override = _mat_brass
	_rivet_mesh.position.y = thickness * 0.5

	# Selection halo (thin torus)
	var base_r: float = shape_data.base_radius() * SCALE_FACTOR + 0.20
	var halo_torus = TorusMesh.new()
	halo_torus.inner_radius = base_r - 0.015
	halo_torus.outer_radius = base_r + 0.015
	halo_torus.rings = 36
	halo_torus.ring_segments = 8
	_halo_mesh.mesh = halo_torus
	_halo_mesh.material_override = _mat_halo
	_halo_mesh.position.y = 0.05
	_halo_mesh.visible = false

	# Rotation handle bead
	var handle_sphere = SphereMesh.new()
	handle_sphere.radius = 0.06
	handle_sphere.height = 0.12
	_handle_mesh.mesh = handle_sphere
	_handle_mesh.material_override = _mat_handle
	_handle_mesh.position = Vector3(0.0, 0.08, -base_r)
	_handle_mesh.visible = false

	add_child(_body_mesh)
	if _rim_mesh.mesh: add_child(_rim_mesh)
	add_child(_rivet_mesh)
	add_child(_halo_mesh)
	add_child(_handle_mesh)

func _process(dt: float):
	if not shape_data: return
	shape_data.step_physics(dt)
	update_from_data()

func update_from_data():
	# Map 2D scene coordinates (1000 x 1600) to 3D world space
	# X: centered at 500 -> [-4.0, +4.0]
	# Z: stage runs from Y=290 to Y=1050 -> mapped onto table [-3.8, +3.8]
	var world_x: float = (shape_data.x - 500.0) * SCALE_FACTOR
	var world_z: float = (shape_data.y - 670.0) * SCALE_FACTOR
	var elev: float = shape_data.elevation
	var world_y: float = 0.08 + elev * 0.35 # Physical lift off the table

	position = Vector3(world_x, world_y, world_z)

	# Dynamic 3D tilt from velocity + rotation
	var rot_y: float = -deg_to_rad(shape_data.rotation)
	var tilt_x: float = deg_to_rad(shape_data.tilt_x)
	var tilt_z: float = deg_to_rad(shape_data.tilt_y)
	rotation = Vector3(tilt_x, rot_y, tilt_z)

	_halo_mesh.visible = is_selected
	_handle_mesh.visible = is_selected

func contains_ray(ray_origin: Vector3, ray_dir: Vector3) -> bool:
	if not shape_data: return false
	# Intersect ray with horizontal plane at token's current height
	var d: float = (position.y - ray_origin.y) / ray_dir.y if absf(ray_dir.y) > 1e-4 else 0.0
	if d <= 0.0: return false
	var hit_pt: Vector3 = ray_origin + ray_dir * d
	var dist_2d: float = Vector2(hit_pt.x - position.x, hit_pt.z - position.z).length()
	var hit_radius: float = shape_data.base_radius() * SCALE_FACTOR + 0.15
	return dist_2d <= hit_radius

func is_on_rotation_handle(ray_origin: Vector3, ray_dir: Vector3) -> bool:
	if not is_selected or not shape_data: return false
	var handle_global: Vector3 = to_global(_handle_mesh.position)
	var d: float = (handle_global.y - ray_origin.y) / ray_dir.y if absf(ray_dir.y) > 1e-4 else 0.0
	if d <= 0.0: return false
	var hit_pt: Vector3 = ray_origin + ray_dir * d
	return hit_pt.distance_to(handle_global) <= 0.22

func start_drag(ray_origin: Vector3, ray_dir: Vector3):
	shape_data.is_dragging = true
	shape_data.target_elevation = 1.0
	var d: float = (0.0 - ray_origin.y) / ray_dir.y if absf(ray_dir.y) > 1e-4 else 0.0
	var hit_pt: Vector3 = ray_origin + ray_dir * d
	_drag_offset_3d = position - hit_pt
	_is_handle_dragging = false
	picked.emit(self)

func start_handle_drag(ray_origin: Vector3, ray_dir: Vector3):
	shape_data.is_rotating = true
	_is_handle_dragging = true
	var d: float = (position.y - ray_origin.y) / ray_dir.y if absf(ray_dir.y) > 1e-4 else 0.0
	var hit_pt: Vector3 = ray_origin + ray_dir * d
	var delta: Vector2 = Vector2(hit_pt.x - position.x, hit_pt.z - position.z)
	_initial_rot_angle = rad_to_deg(delta.angle())
	_initial_shape_rot = shape_data.rotation
	picked.emit(self)

func update_drag_ray(ray_origin: Vector3, ray_dir: Vector3):
	if not shape_data: return
	var d: float = (0.0 - ray_origin.y) / ray_dir.y if absf(ray_dir.y) > 1e-4 else 0.0
	var hit_pt: Vector3 = ray_origin + ray_dir * d

	if _is_handle_dragging:
		var delta: Vector2 = Vector2(hit_pt.x - position.x, hit_pt.z - position.z)
		var cur_angle: float = rad_to_deg(delta.angle())
		var d_angle: float = -(cur_angle - _initial_rot_angle)
		var new_rot: float = ShapeObject.normalize_angle(_initial_shape_rot + d_angle)

		var detent: float = round(new_rot / 45.0) * 45.0
		if absf(ShapeObject.normalize_delta_angle(new_rot - detent)) < 4.5:
			new_rot = ShapeObject.normalize_angle(detent)

		shape_data.target_rotation = new_rot
		rotated.emit(self)
	elif shape_data.is_dragging:
		var target_pos: Vector3 = hit_pt + _drag_offset_3d
		var raw_scene_x: float = target_pos.x / SCALE_FACTOR + 500.0
		var raw_scene_y: float = target_pos.z / SCALE_FACTOR + 670.0

		shape_data.target_x = ShadowCalculator.damp_stage_x(raw_scene_x)
		shape_data.target_y = ShadowCalculator.damp_stage_y(raw_scene_y)
		dragged.emit(self)

func finish_drag():
	if not shape_data: return
	shape_data.is_dragging = false
	shape_data.is_rotating = false
	shape_data.target_elevation = 0.0
	_is_handle_dragging = false
	released.emit(self)
