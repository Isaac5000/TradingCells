# Fondos de maquinas

`themes_atlas.png`: atlas generado con ImageGen integrado el 2026-09-29.
Original: `exec-7adff228-ee39-4b56-8dbe-ed137321b8e3.png`.

Prompt normalizado: atlas 4x3 de paneles pixel art para Minecraft, sin texto,
slots ni controles; centros oscuros tranquilos y marcos de materiales de cada
oficio. Orden: cultivos verdes/madera, cultivos Nether carmesi/cian, cantera
piedra/minerales, cantera Nether negra/roja; criadero abedul/rosa, criadero
carmesi/oro, comercio madera/esmeralda, trueque netherita/oro; hierro remachado,
conversion piedra/morado/verde, simulacion negra/azul electrico, infusor End Stone.

`machine_gui_art.py` recorta y adapta el arte aprobado a cada tamano real de GUI
y REI. El interior excluye los ornamentos sin convertir los marcos en lineas:
24 pixeles de grosor uniforme, extendidos 21 pixeles fuera del rectangulo
original. Se conservan las coordenadas de slots, etiquetas y controles, con
tres pixeles interiores de margen. El panel normalizado de 256x256 se divide
en parches de 56/144/56 pixeles por eje. Las esquinas se reducen por igual en
ambos ejes; las franjas se reducen con esa misma escala y se repiten, recortando
solo el ultimo segmento. No se estiran vetas, remaches ni detalles del marco.

Los PNG se guardan en `gui/container/<block_id>/`, con nombres funcionales:
`background.png`, `background_compact.png`, `background_small.png` y `recipe.png`.
El atlas fuente permanece en `tools/assets/`. `SCREENS` declara el arte y tamano
de cada pantalla, `RECIPE_THEMES` las recetas REI. Por ejemplo,
`container/entity_interaction_farm/background.png` mide 298x270 para su menu de
256x228. En ventanas pequenas se elige otra imagen ya preparada: `_compact`,
grosor 15 y extension 12; o `_small`,
grosor 9 y extension 6. Nunca se deforma el PNG durante el juego. Los resumenes
XP 78x36 conservan su marco interior compacto sin extension. REI reserva el
rectangulo ampliado tanto en menus como en sus recetas. En ejecucion se copia
el PNG; no se dibuja el arte.

Se producen 71 PNG: 58 para menus y resumenes XP, trece para REI. REI utiliza
siempre el marco completo de 200x124 logicos; no se generan variantes compactas
que no tienen consumidores ni fondos REI de comercio manual/simulacion.

Convencion completa y auditoria de consumidores: `docs/GUI_RESOURCES.md`.
Los sprites se ordenan por propietario bajo `gui/sprites/`; `shared/` contiene
solo elementos con varios consumidores. El generador valida el inventario
completo para impedir que reaparezcan rutas antiguas o recursos sin uso.
