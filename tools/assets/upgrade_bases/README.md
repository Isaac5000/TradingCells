# Bases de mejoras

Marcos comunes, 2026-09-21: los cinco niveles de quarry son ahora la unica
carcasa para las cinco familias. El generador extrae cada emblema del PNG
canonico, incluido su contorno, y lo pega sobre el mismo marco por material.
El panel vacio reutiliza texeles limpios de quarry. Oro, madera, acero y cian
del emblema no se recolorean. Resultado completo guardado en los 25 PNG del mod;
ninguna composicion durante el juego. Bases originales conservadas intactas.
La prueba compara cada pixel del marco compartido y del emblema en cada nivel.
La composicion y el recoloreado por codigo fueron autorizados por el usuario.

Terminales, 2026-09-15: `--bake-terminal-steel` cambia solo los pixeles cobrizos
de las dos pantallas y la carcasa compartida a una rampa de acero gris basada en
los tonos de logistics. Conserva coordenadas, alfa, pantallas cian y rejillas.
El resultado completo esta guardado en los tres PNG canonicos y se copia a los
cinco PNG del mod con `--write`; no hay recoloreado durante el juego. Las fuentes
cobrizas permanecen en `originals/terminals/` y permiten reproducir la conversion.
Se ha usado el recoloreado determinista autorizado, no una nueva generacion de
dibujos. La prueba de terminales compara todos los pixeles y su idempotencia.

Correccion de emblemas del 2026-09-13: el mango del pico y los mangos de ambas
espadas usan los cuatro colores opacos de `minecraft:textures/item/stick.png`
de Minecraft 26.2. `--bake-wooden-emblems` guarda esos colores en los PNG fuente.
El generador protege esos pixeles y el oro del lingote/flechas de trueque al
recolorear los tiers. Los PNG finales contienen el resultado completo, sin capas
adicionales en el juego. La forma de los emblemas y los marcos no cambia.

Fuentes canonicas de 64x64 para `generate_family_upgrades.py`. Cada familia aporta
su emblema de cobre; quarry aporta el marco comun y los otros niveles cambian
su paleta. Los cinco PNG originales de `textures/item/upgrades/` se conservan.

Correccion del 2026-09-13: cobre, hierro y oro conservan remaches normales del
material. Solo diamante usa lila y netherite rojo, mediante una mascara limitada
al interior de cada remache. Esta regla sustituye la peticion anterior de poner
lila en todos los niveles salvo netherite.

Las dos mejoras de granja tienen el mismo limite visible de 60x60 dentro del
icono 64x64 que la quarry. La espada esta separada del marco y su recorte canonico
se ha pegado en el PNG de capacidad, con autorizacion expresa del usuario.
El PNG fuente y los cinco PNG finales contienen la espada completa; no existe
composicion al cargar la base ni superposicion durante el juego.
Las pruebas comparan sus pixeles exactos en los cinco niveles. Solo cambian los
indicadores laterales: flechas de velocidad o seis cuadros de capacidad.

Generacion: herramienta integrada ImageGen, septiembre de 2026. Se descarto
la generacion independiente por nivel y la correspondencia entre coordenadas
de dibujos diferentes, que introducia ruido. El usuario autorizo recoloreado
determinista. Las rampas actuales proceden de las paletas originales, ordenadas
por luminancia; no cambian una sombra por un brillo ni introducen tramado.

Prompts originales normalizados (terminales recoloreados despues a acero):

- `pipe`: conservar el icono aprobado por el usuario, con marco de cobre,
  cuatro remaches y tuberia cian en T con flechas laterales.
- `quarry`: conservar tamano, marco y remaches de la tuberia; sustituir solo
  el simbolo central por un pico diagonal grande de acero, sin roca ni flechas.
- `piglin_barter`: mejora de cobre del mismo estilo con un lingote en el
  centro entre dos flechas curvas de intercambio.
- `mob_farm_speed`: editar la base de quarry conservando marco, remaches de
  cobre y proporcion exterior; sustituir el pico por una espada vertical cian
  con acero, mango marron y dos indicadores de velocidad. Reducir la espada
  dentro del panel naranja para que ningun extremo toque el marco.
  ImageGen integrado: `exec-6b551eed-61ae-4d47-ae1e-103d7acd57c6.png`.
- `mob_farm_capacity`: editar la base anterior cambiando solo los indicadores
  laterales por tres cuadros cian en cada lado; misma espada, marco y margenes.
  ImageGen integrado: `exec-683fc085-10b4-47f3-b32f-5aca7857e958.png`.
  `--bake-mob-farm-sword` pega el recorte de velocidad en el PNG fuente de
  capacidad para eliminar las variaciones residuales del modelo generativo.
  Es un paso de preparacion del recurso, no parte de su carga o renderizado.
- `network_terminal`: mismo lenguaje visual, pantalla cian de inventario y
  conexiones de red en el centro.
- `network_crafting_terminal`: mismo lenguaje visual, cuadricula de crafteo
  3x3, flecha y casilla de resultado en una pantalla cian central.
- `network_terminal_body`: carcasa opaca compartida por los bloques colocados;
  marco de cobre, remaches, acero oscuro con ventilacion y dos luces cian.
  ImageGen `exec-6ea8b72c-2d17-43b3-9cd2-89c102bb09bf.png`, con el terminal
  normal como referencia de material. Sin pantalla, texto ni perspectiva.

Para las bases de inventario: pixel art nitido, vista frontal, sin texto ni perspectiva,
fondo realmente transparente. Se extrajo el fondo de las bases que contenian
un damero pintado. La importacion reduce por vecino mas cercano y normaliza
el alfa para sprites opacos con exterior transparente.

Los originales de alta resolucion son material de trabajo; el build solo
valida estas ocho bases pequenas. El juego consume veinticinco variantes, dos
paneles de terminal y una carcasa opaca. Los items usan el mismo modelo del bloque,
sin duplicar PNG de inventario ni modificar los pixeles de los paneles aprobados.
