"""Tier palettes and bounded recoloring of the original essence item centers."""

PALETTES = (((85, 230, 106), (185, 255, 194)), ((53, 207, 255), (183, 243, 255)),
            ((176, 92, 255), (224, 194, 255)), ((255, 211, 78), (255, 242, 166)))


def center_pixels(name, image):
    if image.size != (32, 32):
        raise ValueError("Essence masks use the 32x32 runtime item layout")
    if name == "raw_creature_essence_vial":
        return {(x, y) for y in range(14, 28) for x in range(14, 18 if y < 27 else 17)}
    if name == "entity_essence":
        return {(x, y) for y in range(10, 22) for x in range(10, 22)
                if (pixel := image.getpixel((x, y)))[3] > 0
                and pixel[1] - pixel[0] > 20 and pixel[2] - pixel[0] > 20}
    raise ValueError(f"No essence center mask for {name}")


def variant(name, image, tier):
    base, _ = PALETTES[tier - 1]
    result = image.copy()
    for point in center_pixels(name, image):
        *rgb, alpha = image.getpixel(point)
        low, high = min(rgb), max(rgb)
        # Preserve original shadows and white highlights, replacing only the hue.
        color = tuple(round(low + (high - low) * channel / max(base)) for channel in base)
        result.putpixel(point, (*color, alpha))
    return result
