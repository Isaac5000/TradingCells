"""Bake generated clothing panels into the unmodified vanilla villager UV layout."""

from PIL import Image


def texture(reference, clothing):
    result = Image.new("RGBA", (64, 64))
    # Clothing is solid; only unused UV islands should be transparent.
    clothing = clothing.convert("RGB").convert("RGBA")
    # Keep the recognizable face and nose, but no profession hat or biome overlay.
    result.paste(reference.crop((0, 0, 32, 18)), (0, 0))
    width, height = clothing.size
    panels = [clothing.crop((x * width // 2, y * height // 2,
                            (x + 1) * width // 2, (y + 1) * height // 2))
              for y in range(2) for x in range(2)]

    def paste(panel, bounds, portion=None):
        source = panels[panel]
        if portion:
            source = source.crop(tuple(round(value * size) for value, size in
                                       zip(portion, (source.width, source.height) * 2)))
        left, top, right, bottom = bounds
        result.paste(source.resize((right - left, bottom - top), Image.Resampling.NEAREST), (left, top))

    def wrap(u, v, w, h, d, front, back=None):
        # Cuboid UV net: top/bottom above west, north, east, south faces.
        back = front if back is None else back
        paste(front, (u + d, v, u + d + w, v + d), (0, 0, 1, 0.2))
        paste(back, (u + d + w, v, u + d + 2 * w, v + d), (0, 0.8, 1, 1))
        paste(back, (u, v + d, u + d, v + d + h), (0, 0, 0.4, 1))
        paste(front, (u + d, v + d, u + d + w, v + d + h))
        paste(back, (u + d + w, v + d, u + 2 * d + w, v + d + h), (0.6, 0, 1, 1))
        paste(back, (u + 2 * d + w, v + d, u + 2 * (d + w), v + d + h))

    wrap(16, 20, 8, 12, 6, 0, 1)  # Torso under the coat.
    wrap(0, 38, 8, 20, 6, 0, 1)
    wrap(44, 22, 4, 8, 4, 2)
    wrap(40, 38, 8, 4, 4, 2)
    wrap(0, 22, 4, 12, 4, 3)
    return result
