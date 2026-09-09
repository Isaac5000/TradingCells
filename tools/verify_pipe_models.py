"""Check generated pipe surfaces, all 64 branch layouts, UV joins and shared sprite edges."""
import json
from itertools import product

from PIL import Image

from generate_pipe_textures import FRAMES, KINDS, base_texture, frame, sprite_path

DIRECTIONS = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1),
              "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}
EDGES = ("north", "east", "south", "west")
FACE_AXES = {"up": (0, 2, 1, 1), "down": (0, 2, 1, -1), "north": (0, 1, -1, -1),
             "south": (0, 1, 1, -1), "west": (2, 1, 1, -1), "east": (2, 1, -1, -1)}


def transform(point, rotation, centered=True):
    offset = 8 if centered else 0
    x, y, z = (value - offset for value in point)
    for _ in range(rotation.get("x", 0) // 90):
        y, z = z, -y
    for _ in range(rotation.get("y", 0) // 90):
        x, z = -z, x
    return x + offset, y + offset, z + offset


def direction_after(direction, rotation):
    vector = transform(DIRECTIONS[direction], rotation, False)
    return next(name for name, value in DIRECTIONS.items() if value == vector)


def surface_units(cube, direction):
    axis = next(i for i, value in enumerate(DIRECTIONS[direction]) if value)
    sign = DIRECTIONS[direction][axis]
    axes = [i for i in range(3) if i != axis]
    for a in range(cube["from"][axes[0]], cube["to"][axes[0]]):
        for b in range(cube["from"][axes[1]], cube["to"][axes[1]]):
            point = [0, 0, 0]
            point[axis] = cube["to" if sign == 1 else "from"][axis]
            point[axes[0]], point[axes[1]] = a + 0.5, b + 0.5
            yield tuple(point)


def expected_surface(state):
    voxels = set(product(range(5, 11), repeat=3))
    for direction, connection in state.items():
        if connection == "none":
            continue
        # Rotate the north-oriented arm and optional extractor collar around the center.
        rotations = {"north": {}, "south": {"y": 180}, "west": {"y": 270},
                     "east": {"y": 90}, "up": {"x": 270}, "down": {"x": 90}}
        for x, y, z in product(range(4, 12), range(4, 12), range(5)):
            if 5 <= x < 11 and 5 <= y < 11 or connection == "extract" and z == 0:
                center = transform((x + 0.5, y + 0.5, z + 0.5), rotations[direction])
                voxels.add(tuple(int(value - 0.5) for value in center))
    result = set()
    for voxel in voxels:
        for direction, normal in DIRECTIONS.items():
            neighbor = tuple(a + b for a, b in zip(voxel, normal))
            if neighbor in voxels:
                continue
            point = tuple(a + 0.5 + b / 2 for a, b in zip(voxel, normal))
            axis = next(i for i, value in enumerate(normal) if value)
            if (state[direction] == "pipe" and point[axis] in (0, 16)
                    and all(5 <= point[i] <= 11 for i in range(3) if i != axis)):
                continue
            result.add((direction, point))
    return result


def verify(resources):
    models = {key.removeprefix("assets/trading_cells/models/").removesuffix(".json"): json.loads(value)
              for key, value in resources.items() if "/models/block/" in key}
    for name, model in models.items():
        if "_arm_" not in name:
            continue
        for cube in model["elements"]:
            axis = next(i for i in range(3) if cube["from"][i] != 5 or cube["to"][i] != 11)
            for normal, face in cube["faces"].items():
                u, v, u_sign, v_sign = FACE_AXES[normal]
                u0, v0, u1, v1 = face["uv"]
                for a, b in ((0.2, 0.7), (0.8, 0.3)):
                    point = [(cube["from"][i] + cube["to"][i]) / 2 for i in range(3)]
                    for dimension, fraction, sign in ((u, a, u_sign), (v, b, v_sign)):
                        progress = fraction if sign > 0 else 1 - fraction
                        point[dimension] = cube["from"][dimension] + progress * (cube["to"][dimension] - cube["from"][dimension])
                    actual_u = u0 + (u1 - u0) * (b if face["rotation"] == 90 else a)
                    actual_v = v0 + (v1 - v0) * (1 - a if face["rotation"] == 90 else b)
                    across = v if axis == u else u
                    assert abs(actual_u - point[axis]) < 1e-8, (name, normal, "animation phase reset")
                    assert abs(actual_v - (point[across] - 5) * 16 / 6) < 1e-8, (name, normal, "stripe offset")
    for kind in KINDS:
        states = json.loads(resources[f"assets/trading_cells/blockstates/{kind}_pipe.json"])["multipart"]
        for mask in range(64):
            for connection in ("pipe", "insert", "extract"):
                state = {side: connection if mask & (1 << i) else "none"
                         for i, side in enumerate(DIRECTIONS)}
                actual = set()
                for part in states:
                    if not all(state[side] in value.split("|") for side, value in part["when"].items()):
                        continue
                    applied = part["apply"]
                    model = models[applied["model"].removeprefix("trading_cells:")]
                    for cube in model["elements"]:
                        for direction, face in cube["faces"].items():
                            normal = direction_after(direction, applied)
                            if applied["model"].endswith("_cap"):
                                assert state[normal] in ("insert", "extract"), (kind, state, "internal pipe cap")
                                assert direction_after(face["cullface"], applied) == normal, "Machine caps use neighbor occlusion"
                                assert model["textures"]["pipe"] == "trading_cells:block/logistics/pipe_cap", "Machine caps share the static dark texture"
                            for point in surface_units(cube, direction):
                                unit = normal, transform(point, applied)
                                assert unit not in actual, (kind, state, "overlapping face", unit)
                                actual.add(unit)
                            if "_core_" in applied["model"]:
                                texture = model["textures"]["pipe"].rsplit("/", 1)[1]
                                local = int(texture.rsplit("_", 1)[1])
                                u, v, _, _ = FACE_AXES[normal]
                                for bit, (axis, sign) in enumerate(((v, -1), (u, 1), (v, 1), (u, -1))):
                                    edge = next(name for name, vector in DIRECTIONS.items() if vector[axis] == sign)
                                    assert bool(local & (1 << bit)) == (state[edge] != "none"), (
                                        kind, state, "disconnected texture", normal, edge)
                expected = expected_surface(state)
                assert actual == expected, (kind, state, "surface mismatch", len(actual - expected), len(expected - actual))
    # A joined edge has exactly the transverse bevel, never its own end highlight.
    straight = base_texture(10)
    for mask in range(16):
        surface = base_texture(mask)
        for bit in range(4):
            if not mask & (1 << bit):
                continue
            for p in range(6, 26):
                point = ((p, 0), (31, p), (p, 31), (0, p))[bit]
                assert surface.getpixel(point) == straight.getpixel((0, p))
    for kind in KINDS:
        with Image.open(sprite_path(f"{kind}_pipe_core_15")) as sprite:
            assert sprite.getpixel((0, 5)) == sprite.getpixel((5, 0)) == (116, 118, 121, 255)
        images = [frame(kind, index, straight) for index in range(FRAMES)]
        for mask in range(16):
            with Image.open(sprite_path(f"{kind}_pipe_core_{mask}")) as sprite:
                for bit in range(4):
                    if not mask & (1 << bit):
                        continue
                    for index, source in enumerate(images):
                        for p in range(11, 21):
                            x, y = ((p, 0), (31, p), (p, 31), (0, p))[bit]
                            along = 10 if bit in (0, 3) else 21
                            assert sprite.getpixel((x, y + index * 32)) == source.getpixel((along, p)), (
                                kind, mask, bit, index, "junction animation discontinuity")
        for index in range(FRAMES):
            if kind == "energy":
                continue  # Redstone pulses in place rather than flowing.
            for x in range(32):
                for y in range(11, 21):
                    assert images[(index + 1) % FRAMES].getpixel((x, y)) == images[index].getpixel(((x - 4) % 32, y)), (
                        kind, index, "animation does not continue into the next block/frame")
    print("Pipe models: 960 layouts, machine caps, exterior-only surfaces, aligned UVs and all animation joins verified")
