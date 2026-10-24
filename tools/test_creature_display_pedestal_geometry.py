#!/usr/bin/env python3
"""Regression checks for pedestal geometry, uniform trim and item transforms.

Run: python tools/test_creature_display_pedestal_geometry.py
"""
from __future__ import annotations

import json
import unittest
from pathlib import Path

from generate_creature_display_pedestal_textures import METAL, SIZE, metal

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "src/main/resources/assets/trading_cells/models/block/creature_display_pedestal.json"


class CreatureDisplayPedestalGeometryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.model = json.loads(MODEL.read_text(encoding="utf-8"))

    def test_all_cuboids_fit_inside_one_block(self) -> None:
        for i, element in enumerate(self.model["elements"]):
            for axis in range(3):
                with self.subTest(cuboid=i, axis=axis):
                    self.assertGreaterEqual(element["from"][axis], 0)
                    self.assertLessEqual(element["to"][axis], 16)
                    self.assertLess(element["from"][axis], element["to"][axis])

    def test_no_cuboids_have_overlapping_volumes(self) -> None:
        elements = self.model["elements"]
        for i, a in enumerate(elements):
            for j in range(i + 1, len(elements)):
                b = elements[j]
                penetration = [
                    min(a["to"][axis], b["to"][axis])
                    - max(a["from"][axis], b["from"][axis])
                    for axis in range(3)
                ]
                with self.subTest(cuboids=(i, j)):
                    self.assertFalse(all(value > 0.000001 for value in penetration),
                                     f"Overlapping cuboids {i} and {j}: {penetration}")

    def test_metal_has_uniform_color_and_no_directional_shading(self) -> None:
        self.assertFalse(self.model["ambientocclusion"])
        self.assertEqual({metal(x, y) for x in range(SIZE) for y in range(SIZE)}, {METAL})
        for i, element in enumerate(self.model["elements"]):
            if any(face.get("texture") == "#trim" for face in element["faces"].values()):
                with self.subTest(cuboid=i):
                    self.assertFalse(element.get("shade", True))
                    self.assertEqual(
                        {face["texture"] for face in element["faces"].values()}, {"#trim"}
                    )

    def test_drop_footprint_matches_a_normal_block(self) -> None:
        # A normal full-cube block displayed on the ground occupies 8 model
        # units (16 voxels at the standard 0.5 ground transform). The pedestal
        # has inset geometry, so compare rendered extents rather than a
        # literal transformation value that would fail after a geometry edit.
        ground = self.model["display"]["ground"]
        scale = ground["scale"]
        self.assertEqual(len(scale), 3)
        self.assertEqual(scale, [scale[0]] * 3, "Do not distort the dropped pedestal")
        normal_block_span = 16 * 0.5
        for axis in range(3):
            minimum = min(element["from"][axis] for element in self.model["elements"])
            maximum = max(element["to"][axis] for element in self.model["elements"])
            rendered_span = (maximum - minimum) * scale[axis]
            with self.subTest(axis=axis):
                self.assertGreaterEqual(rendered_span, normal_block_span * 0.75)
                self.assertLessEqual(rendered_span, normal_block_span * 1.05)


if __name__ == "__main__":
    unittest.main()