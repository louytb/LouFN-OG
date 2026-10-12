extends Node3D

var player: CharacterBody3D
var camera: Camera3D
var hud: CanvasLayer
var hp := 100
var ammo := 30
var bots: Array[Node3D] = []
var built_count := 0
var fire_timer := 0.0
var status_label: Label
var ammo_label: Label

func _ready() -> void:
	_make_world()
	_make_player()
	_make_bots()
	_make_hud()

func _process(delta: float) -> void:
	fire_timer = maxf(0.0, fire_timer - delta)
	for bot in bots:
		if not is_instance_valid(bot):
			continue
		var dir := player.global_position - bot.global_position
		dir.y = 0
		if dir.length() > 2.5:
			bot.global_position += dir.normalized() * delta * 1.0
		else:
			hp = maxi(0, hp - int(delta * 4.0))
	status_label.text = "HP %d   BOTS %d   WALLS %d" % [hp, bots.size(), built_count]
	ammo_label.text = "AMMO %d" % ammo

func _make_world() -> void:
	var env := WorldEnvironment.new()
	var e := Environment.new()
	e.background_mode = Environment.BG_SKY
	e.sky = Sky.new()
	e.sky.sky_material = ProceduralSkyMaterial.new()
	e.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	e.ambient_light_color = Color(0.65, 0.72, 0.85)
	e.ambient_light_energy = 0.8
	env.environment = e
	add_child(env)
	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-55, -30, 0)
	add_child(sun)
	_box("Ground", Vector3(0,-0.5,0), Vector3(70,1,70), Color(0.24,0.48,0.28))
	for i in range(12):
		var x := float((i % 4) * 12 - 18)
		var z := float(int(i / 4) * 12 - 12)
		_box("RetroBlock", Vector3(x,0.8,z), Vector3(3,1.6,3), [Color(0.62,0.43,0.25),Color(0.35,0.52,0.62),Color(0.72,0.64,0.37)][i%3])

func _make_player() -> void:
	player = CharacterBody3D.new()
	player.name = "Player"
	player.position = Vector3(0,1,8)
	add_child(player)
	var shape := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.45
	capsule.height = 1.8
	shape.shape = capsule
	shape.position.y = 0.9
	player.add_child(shape)
	var body := MeshInstance3D.new()
	var mesh := CapsuleMesh.new()
	mesh.radius = 0.45
	mesh.height = 1.8
	body.mesh = mesh
	body.position.y = 0.9
	body.material_override = _mat(Color(0.12,0.45,0.95))
	player.add_child(body)
	camera = Camera3D.new()
	camera.position = Vector3(0,4.5,7)
	camera.rotation_degrees = Vector3(-22,0,0)
	camera.current = true
	player.add_child(camera)

func _make_bots() -> void:
	for i in range(5):
		var bot := Node3D.new()
		bot.name = "Bot"
		bot.position = Vector3(float(i*5-10),0.9,float(-8-(i%2)*7))
		add_child(bot)
		var mesh := MeshInstance3D.new()
		var cm := CapsuleMesh.new()
		cm.radius = 0.45
		cm.height = 1.8
		mesh.mesh = cm
		mesh.material_override = _mat(Color(0.9,0.25,0.18))
		bot.add_child(mesh)
		bots.append(bot)

func _make_hud() -> void:
	hud = CanvasLayer.new()
	add_child(hud)
	var title := Label.new()
	title.text = "LOUFN OG  •  RETRO ARENA"
	title.position = Vector2(18,12)
	title.add_theme_font_size_override("font_size",24)
	title.add_theme_color_override("font_color",Color(1,0.86,0.28))
	hud.add_child(title)
	status_label = Label.new()
	status_label.position = Vector2(18,46)
	status_label.add_theme_font_size_override("font_size",20)
	hud.add_child(status_label)
	ammo_label = Label.new()
	ammo_label.position = Vector2(18,76)
	ammo_label.add_theme_font_size_override("font_size",20)
	hud.add_child(ammo_label)
	_button("▲",Vector2(95,370),Vector2(70,70),func(): _move(Vector2(0,-1)))
	_button("▼",Vector2(95,455),Vector2(70,65),func(): _move(Vector2(0,1)))
	_button("◀",Vector2(20,415),Vector2(70,65),func(): _move(Vector2(-1,0)))
	_button("▶",Vector2(170,415),Vector2(70,65),func(): _move(Vector2(1,0)))
	_button("JUMP",Vector2(690,420),Vector2(100,70),func(): _jump())
	_button("FIRE",Vector2(810,355),Vector2(115,80),func(): _fire())
	_button("BUILD",Vector2(810,450),Vector2(115,65),func(): _build())
	_button("RESTART",Vector2(780,18),Vector2(150,50),func(): get_tree().reload_current_scene())

func _button(t: String, p: Vector2, s: Vector2, action: Callable) -> void:
	var b := Button.new()
	b.text = t
	b.position = p
	b.size = s
	b.add_theme_font_size_override("font_size",20)
	b.pressed.connect(action)
	hud.add_child(b)

func _move(v: Vector2) -> void:
	player.velocity.x = v.x * 6.0
	player.velocity.z = v.y * 6.0
	player.move_and_slide()

func _jump() -> void:
	if player.is_on_floor():
		player.velocity.y = 7.0

func _fire() -> void:
	if fire_timer > 0.0 or ammo <= 0:
		return
	fire_timer = 0.22
	ammo -= 1
	var nearest: Node3D
	var best := 18.0
	for bot in bots:
		if not is_instance_valid(bot):
			continue
		var d := camera.global_position.distance_to(bot.global_position)
		if d < best:
			best = d
			nearest = bot
	if nearest:
		bots.erase(nearest)
		nearest.queue_free()

func _build() -> void:
	var p := player.global_position + (-player.global_basis.z) * 2.5
	p.y = 1.2
	_box("BuildWall",p,Vector3(2.8,2.4,0.35),Color(0.45,0.72,0.92))
	built_count += 1

func _box(n: String, p: Vector3, sz: Vector3, c: Color) -> void:
	var body := StaticBody3D.new()
	body.name = n
	body.position = p
	add_child(body)
	var mesh := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = sz
	mesh.mesh = bm
	mesh.material_override = _mat(c)
	body.add_child(mesh)
	var cs := CollisionShape3D.new()
	var bs := BoxShape3D.new()
	bs.size = sz
	cs.shape = bs
	body.add_child(cs)

func _mat(c: Color) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = c
	m.roughness = 0.9
	return m

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and not event.echo:
		match event.keycode:
			KEY_W: _move(Vector2(0,-1))
			KEY_S: _move(Vector2(0,1))
			KEY_A: _move(Vector2(-1,0))
			KEY_D: _move(Vector2(1,0))
			KEY_SPACE: _jump()
			KEY_B: _build()
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		_fire()

func _physics_process(delta: float) -> void:
	if not player:
		return
	if not player.is_on_floor():
		player.velocity.y -= 18.0 * delta
	else:
		player.velocity.y = maxf(player.velocity.y,0.0)
	player.move_and_slide()
