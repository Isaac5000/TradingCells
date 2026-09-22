# Texturas de objetos: 2026-09-21

## Fuentes y resultado

Las cinco texturas finales viven en
`src/main/resources/assets/trading_cells/textures/item/`:

- `villager_capturer.png`
- `piglin_capturer.png`
- `unbreakable_villager_capturer.png`
- `unbreakable_piglin_capturer.png`
- `storm_shard.png`

Generacion y edicion: ImageGen integrado, no CLI ni API externa. El usuario
autorizo el ajuste posterior por codigo a 64x64, vecino mas cercano, sin
suavizado. El alfa se normalizo a 0/255 con umbral 128; el exterior y el hueco
del mango permanecen transparentes. Los PNG anteriores estan guardados en
`artifacts/item-texture-drafts-20260921/originals/`.

Los originales generados y la hoja `integrated-preview.png` estan en
`artifacts/item-texture-drafts-20260921/`. No forman parte del JAR. Los nombres
del mod, recetas, modelos de captura, datos y comportamiento no cambian.

## Prompts finales

- Aldeano: frasco de captura frontal de pixel art Minecraft, simetria bilateral,
  silueta homogenea y centrada, vidrio verde y perla de Ender, collares y tapa de
  hierro, dos soportes iguales; referencias: capturador original y receta.
  Cuadricula logica de 64 pixeles, fondo transparente, sin texto ni ornamentos
  de netherita en la version normal.
- Piglin: editar el frasco anterior conservando posiciones, escala y contorno;
  hierro a oro, camara verde a rojo/naranja, soportes de piedra negra; usar
  capturador original de piglins solo como referencia de paleta.
- Irrompible aldeano: mismo frasco con jaula de obsidiana llorosa negra/lila.
  Correccion final: sustituir toda la tapa rectangular y su insignia por un
  mango metalico real de cinco puntas, plata/lavanda, simetrico, unido al cuello
  y con hueco central. Sin rectangulo detras, estrella flotante ni PNG de estrella
  del Nether pegado. Conservar camara verde, cuello y resto del cuerpo.
  Fuente final: `exec-1b973da6-fcd5-4970-b54c-b0863d3ea3ef.png`.
- Irrompible piglin: editar la version anterior; camara y detalles verdes a
  rojo/naranja, collar del cuello a oro. Conservar mango de estrella plata/lavanda,
  hueco, silueta y posiciones; obsidiana lila intacta. Fondo transparente.
  Fuente final: `exec-82ff2ca2-9e0d-44e6-a267-c1a3eeae8668.png`.
- Fragmento de tormenta: fragmento alto e irregular de cristal verde electrico,
  silueta dentada, fisuras blancas/lima y pequenos reflejos cian. Pixel art nitido,
  contorno verde bosque, margenes transparentes. No estrella de cinco puntas ni
  aura exterior pintada: el juego mantiene su propia capa de creeper cargado.
  Fuente: `exec-826b41e1-dd5d-4c21-99f2-6a8d1c48f8ab.png`.

## Energia y marcos

`StormShardItemRenderSupport` obtiene la silueta del sprite real al hornear el
modelo, incluido tras recargar recursos. Conserva `RenderTypes.energySwirl` y
la textura vanilla del creeper cargado; sin mascara antigua de estrella, sin
lectura de imagenes por fotograma.
El modelo del fragmento se marca como animado en `ModifyBakingResult` para que
la cache de inventario no congele la capa. La comprobacion Vulkan compara dos
frames: 678 pixeles cambian, exclusivamente dentro de la silueta del fragmento.

Los 25 PNG de mejoras comparten los cinco marcos de quarry. Los emblemas se
extraen y componen durante la generacion, nunca durante el juego. Detalles y
prompts previos: `tools/assets/upgrade_bases/README.md`.

Correccion del 2026-09-22: el emblema de trueque usa contornos por filas del
PNG original, no poligonos amplios que copiaban fragmentos del panel de cobre.
Lingote y flechas mantienen brillos blancos y sombras doradas identicos en los
cinco materiales. Prueba comprueba zonas de fondo y continuidad del oro.
Previsualizacion: `artifacts/piglin-cleanup-20260922/piglin-after.png`.
Captura real OpenGL de las cinco variantes y resto de familias:
`artifacts/piglin-cleanup-client-opengl-20260922/run-1/result/capture.png`.

Validacion dirigida: `testLogisticsResourceValidation`, `checkUpgradeFamilyTextures`,
`checkGraphicsBackendIndependence`; fixture real `item-textures` con dos capturas
para comprobar la animacion. Fixture `pipe-hands` comprueba ambos agarres.
