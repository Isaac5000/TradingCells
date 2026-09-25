"""Authored injector geometry and UVs; dimensions share the existing hand/animation space."""

from copy import deepcopy
from math import sqrt

ATLAS = "trading_cells:item/essence/injector_atlas"
SIDES = ("north", "south", "west", "east", "up", "down")


def tile(index, inset=0.04):
    x, y = index % 4 * 4, index // 4 * 4
    return [x + inset, y + inset, x + 4 - inset, y + 4 - inset]


def cube(start, end, material, *, rotation=None, faces=None):
    element = {"from": start, "to": end, "faces": {
        side: {"texture": "#atlas", "uv": tile((faces or {}).get(side, material))}
        for side in SIDES}}
    if rotation:
        element["rotation"] = rotation
    return element


def bevelled(start, end, bevel, material, axis="z"):
    """Union of closed solids with clipped corners; inset caps avoid coplanar overlap."""
    axis_index = "xyz".index(axis)
    u, v = [i for i in range(3) if i != axis_index]
    vertical_from, vertical_to = list(start), list(end)
    vertical_from[u] += bevel
    vertical_to[u] -= bevel
    horizontal_from, horizontal_to = list(start), list(end)
    horizontal_from[v] += bevel
    horizontal_to[v] -= bevel
    horizontal_from[axis_index] += 0.006
    horizontal_to[axis_index] -= 0.006
    result = [cube(vertical_from, vertical_to, material), cube(horizontal_from, horizontal_to, material)]
    for center_u in (start[u] + bevel, end[u] - bevel):
        for center_v in (start[v] + bevel, end[v] - bevel):
            low, high, origin = list(start), list(end), [(a + b) / 2 for a, b in zip(start, end)]
            low[u], high[u] = center_u - bevel / sqrt(2), center_u + bevel / sqrt(2)
            low[v], high[v] = center_v - bevel / sqrt(2), center_v + bevel / sqrt(2)
            low[axis_index] += 0.012
            high[axis_index] -= 0.012
            origin[u], origin[v] = center_u, center_v
            result.append(cube([round(a, 5) for a in low], [round(a, 5) for a in high], material,
                               rotation={"origin": origin, "axis": axis, "angle": 45}))
    return result


def body():
    result = bevelled([2.3, 6.8, 6.6], [10.25, 9.5, 9.4], 0.5, 1)
    result += bevelled([2.75, 2, 6.8], [4.9, 7.15, 9.2], 0.3, 2)
    # Two inset side plates have their own UVs, rather than one repeated block surface.
    for z0, z1 in ((6.54, 6.66), (9.34, 9.46)):
        result.append(cube([3.1, 7.08, z0], [9.35, 9.2, z1], 0))
    for z0, z1 in ((6.73, 6.82), (9.18, 9.27)):
        result.append(cube([3.25, 2.65, z0], [4.4, 5.95, z1], 3))
    result.append(cube([5.5, 9.5, 7.05], [9.15, 9.66, 8.95], 11))
    result += bevelled([1.65, 7.3, 7.1], [2.6, 9.0, 8.9], 0.25, 15)
    # Coaxial beveled collars narrow into a stepped, solid needle.
    result += bevelled([10.15, 7.15, 6.95], [11.05, 9.15, 9.05], 0.35, 14, "x")
    result += bevelled([11.02, 7.55, 7.35], [11.7, 8.75, 8.65], 0.24, 8, "x")
    result.append(cube([11.65, 8.01, 7.86], [15.35, 8.29, 8.14], 13))
    result.append(cube([15.35, 8.08, 7.93], [15.8, 8.22, 8.07], 13))
    result += bevelled([4.75, 4.95, 7.6], [7.1, 5.35, 8.4], 0.14, 5)
    result += bevelled([6.7, 5.2, 7.6], [7.12, 6.95, 8.4], 0.14, 5)
    result.append(cube([5.05, 5.85, 7.78], [5.42, 7.08, 8.22], 12,
                       rotation={"origin": [5.24, 7.03, 8], "axis": "z", "angle": -22.5}))
    result += bevelled([3.0, 9.44, 6.9], [5.2, 9.95, 9.1], 0.35, 5, "y")
    return result


def vial():
    result = bevelled([3.27, 9.95, 7.17], [4.93, 10.2, 8.83], 0.25, 8, "y")
    # Four thin transparent walls leave the liquid visible from either hand.
    for low, high in (([3.3, 10.16, 7.2], [4.9, 13.0, 7.22]),
                      ([3.3, 10.16, 8.78], [4.9, 13.0, 8.8]),
                      ([3.3, 10.16, 7.22], [3.32, 13.0, 8.78]),
                      ([4.88, 10.16, 7.22], [4.9, 13.0, 8.78])):
        result.append(cube(low, high, 9, faces={"up": 10, "down": 10}))
    result += bevelled([3.15, 12.96, 7.05], [5.05, 13.3, 8.95], 0.3, 8, "y")
    result += bevelled([3.35, 13.25, 7.25], [4.85, 14.15, 8.75], 0.25, 6, "y")
    result += bevelled([3.55, 14.14, 7.45], [4.65, 14.55, 8.55], 0.24, 7, "y")
    return result


def shifted(elements, dx, dy=0):
    result = deepcopy(elements)
    for element in result:
        points = [element["from"], element["to"]]
        if "rotation" in element:
            points.append(element["rotation"]["origin"])
        for point in points:
            point[0] = round(point[0] + dx, 5)
            point[1] = round(point[1] + dy, 5)
    return result


def liquid(fill):
    if fill <= 0:
        return []
    element = cube([3.4, 10.21, 7.3], [4.8, round(10.21 + 2.68 * fill, 5), 8.7], 0)
    element["faces"] = {side: {"texture": "#essence", "uv": [1, 7, 2, 8]} for side in SIDES}
    return [element]
