# Emblemas vanilla

- `clock_vanilla.png`: `assets/minecraft/textures/item/clock_00.png`, extraido
  sin recolorear de Minecraft 26.2.
- `chest_vanilla.png`: render GUI real de `minecraft:chest`, exportado por la
  fixture `vanilla-icons` a 1280x720, escala GUI 2. Recorte `(160,160,224,224)`;
  solo el fondo magenta pasa a transparente. No es una ilustracion generada.
- `bundle_vanilla.png`: `assets/minecraft/textures/item/bundle.png`, extraido
  sin cambios de Minecraft 26.2. Sustituye al cofre en capacidad desde 2026-10-01;
  el cofre queda como fuente historica, no se distribuye como mejora.

`generate_family_upgrades.py --bake-utility-emblems --write --check` centra ambos
iconos activos (reloj y bundle) con vecino mas cercano en 28x28 como maximo,
saturacion 0.80 y contraste 1.10, sobre los marcos 64x64
existentes. El resultado queda integrado en los PNG fuente y en los diez PNG
distribuidos. No se dibujan capas extra durante el juego.
