# Creature Generator Art

Source `atlas.png`: generated with the built-in image generation tool on
2026-10-08. Four opaque quadrant textures, cropped and reduced with nearest
neighbor to 16x16. No vanilla or Essence Workbench textures used.

Regenerate using `python tools/generate_creature_generator_resources.py`.
Runtime PNGs: `src/main/resources/assets/trading_cells/textures/block/creature_generator/`.
The exporter also writes the static pedestal model; collision boxes live in
`CreatureGeneratorBlock`. No runtime texture generation or per-PNG CI checks.

## Generation Prompt

Create a production Minecraft pixel-art block texture atlas, a square image
divided into EXACTLY four equal square texture tiles in a 2 by 2 grid, no
padding, no separation lines, no text, no letters, no render of a 3D object,
no perspective. Each tile must use a coarse 16 by 16 logical pixel grid with
perfectly flat hard pixel edges, enlarged nearest-neighbor. This is a futuristic
egg-incubation workbench named Creature Generator. Palette: near black, cool
medium gray brushed metal, vivid azure-cyan accents, ivory egg shell and small
amber status details. TOP LEFT TILE: top-down tabletop panel, dark gray metal
square with cyan rim, a central small ivory egg-shaped symbol on a dark circular
incubation socket, four tiny cyan corner indicators; at actual 16x16 readability
more important than ornament. TOP RIGHT TILE: front face, charcoal metal housing
with a small central dark display containing an ivory egg silhouette and three
small azure status bars beneath, squared cyan trims at left and right, not text.
BOTTOM LEFT TILE: plain dark gray machinery casing, subtly stepped metal panels
and two small ventilation slits, no egg and no bright center. BOTTOM RIGHT TILE:
narrow azure-blue illuminated trim pattern on near-black metal, symmetric along
both axes, useful for small pedestal edge surfaces. Completely opaque. Every
tile fully fills its quadrant. Flat lighting baked into crisp pixel shading,
no gradients or noise, consistent Minecraft vanilla-scale pixel density. Unique
purpose-built texture art, not vanilla blocks pasted together.
