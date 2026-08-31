# Checklist de publicacion 1.0.0

## Automatizacion

- [x] Ejecutar `clean releaseCheck` dos veces con JAR identico (`2026-08-31`, 26/26 GameTests).
- [x] Ejecutar `./gradlew runServer` y confirmar que alcanza `Done` sin errores de Trading Cells (`2026-08-25`).
- [x] Comprobar que `build/libs/trading_cells-1.0.0.jar` es el unico artefacto que se publica.
- [ ] Confirmar la CI del commit definitivo en Ubuntu, Windows y macOS.
- [ ] Instalar ese JAR en una instancia limpia, no el classpath de desarrollo.

## Cliente y graficos

- [ ] Abrir OpenGL con REI y sin REI; confirmar `Using graphics backend OpenGL`.
- [ ] Abrir Vulkan con REI y sin REI; confirmar `Using graphics backend Vulkan` y no un fallback.
- [ ] Comparar renderizadores, transparencias, glint, scissor, desplegables y tooltips.
- [ ] Revisar ingles y espanol con escalas GUI 2, 3 y 4 en `1424x855` y `1920x1080`.
- [ ] Abrir cada pantalla y categoria REI; probar nombres largos procedentes de mods.

## Funcionalidad

- [ ] Probar cada maquina activa, inactiva, sin entrada, bloqueada y con salidas llenas.
- [ ] Probar tolvas y tuberias por todas las caras, incluyendo objetos y XP liquido.
- [ ] Probar Trader y Autotrader en multijugador: descuentos, restock, reset, seleccion, XP e intercambio masivo de 2304 operaciones.
- [ ] Probar todos los objetivos, filtros, pausa, espadas y botin de las Granjas de Esqueletos, Zombis, Saqueadores y Creepers.
- [ ] Probar Decapitacion I-VI y niveles de comandos: mundo, granjas, herreria y cabezas externas.
- [ ] Probar Almacen e Infusor en limites de nivel, `Integer.MAX_VALUE`, fluido y salida ocupada sin perdida de XP.
- [ ] Probar Toque de Seda II en Infusor y yunque, los diez bloques y las tres clases de herramienta documentadas.
- [ ] Confirmar que cada bloque especial solo obtiene velocidad eficiente con su herramienta correcta y Toque de Seda II.
- [ ] Romper y recolocar generador, generador de desafio, arca, tarta parcial y bloques sospechosos comprobando sus datos.
- [ ] Convertir un generador simple y otro modificado a huevo: vista fija, ausencia de advertencia confiable, tooltip, brillo, consumo unico, equipo, efectos, pasajeros y montura.
- [ ] Completar una prueba normal y una ominosa con uno y varios jugadores; verificar recompensas, suspension por distancia, criaturas fugadas, salida/reentrada y bloqueo de reinicio por redstone.
- [ ] Reabrir una misma arca normal y ominosa varias veces con el mismo jugador, comprobar que no guarda UUID y revisar el nombre localizado del objeto ominoso.
- [ ] Verificar que arena y grava sospechosas conservan contenido pero reinician el progreso de cepillado.
- [ ] Probar recarga de datapacks y fallback de profesiones, POI, cultivos, tiers, criaturas y botin dinamicos.
- [ ] Probar descriptor de granja valido, invalido, sobrescrito por prioridad, con tags y recuperacion vanilla global.
- [ ] Probar Fortuna 0, 3, 7 y 255 en fruto y semilla de trigo, remolacha, plantorcha y planta odre.
- [ ] Probar REI, More Villagers y al menos un mod que amplie cada catalogo relevante.
- [ ] Probar con Jade y sin Jade: datos de maquinas, entidad del generador y arranque de cliente/servidor sin dependencia obligatoria.

## Persistencia

- [ ] Abrir una copia de un mundo existente.
- [ ] Romper y recolocar todas las maquinas conservando inventario, entidad, ofertas, XP, filtros, progreso y bloque pendiente.
- [ ] Descargar y recargar chunks durante procesos activos y bloqueados.
- [ ] Comparar NBT antes y despues y conservar la copia de seguridad original sin abrir.

## Publicacion

- [x] Revisar `README.md`, `CHANGELOG.md`, configuracion y plantilla de CurseForge (`2026-08-25`).
- [ ] Completar imagenes, enlace de soporte y permiso de modpacks en la plantilla.
- [ ] Verificar recetas, traducciones, modelos y pestañas creativas en la instancia limpia.
- [ ] Archivar registros de la matriz manual y las mediciones aceptadas junto a la version.

## Informe de ejecucion

Cada fila debe incluir fecha, backend, mods, mundo o plantilla, resultado y un
enlace o ruta al registro. No se marca una combinacion como aprobada solo por
haber solicitado el backend: el log debe confirmar el backend efectivo.

| Fecha | Prueba/backend | Mods | Mundo/plantilla | Resultado | Registro |
| --- | --- | --- | --- | --- | --- |
| 2026-08-31 | `clean releaseCheck` x2 | Trading Cells GameTests | Mundo temporal | Aprobado; 26/26; JAR reproducible `14752C37...B3A103` | `docs/releases/1.0.0-validation.md` |
| 2026-08-31 | Arranque Vulkan y carga de recursos | REI, Jade, Trading Cells | `Test` | Backend Vulkan real; suelo de Slimes opaco; sin errores de recursos de Trading Cells | `run/vulkan/logs/latest.log` |
| 2026-08-31 | Menu de Granja de Piglins, Vulkan | REI, Jade, Trading Cells | `Test` | Aprobado; menu estable y sin NPE | `run/vulkan/logs/latest.log` |
| 2026-08-30 | `clean releaseCheck` x2 | Trading Cells GameTests | Mundo temporal | Aprobado; 23/23 y JAR reproducible | `docs/releases/1.0.0-validation.md` |
| 2026-08-30 | Arranque cliente OpenGL | REI, Jade, Trading Cells | Cliente de desarrollo | Menu principal; registros cliente cargados | `docs/releases/1.0.0-validation.md` |
| 2026-08-29 | `clean releaseCheck` x2 | Trading Cells GameTests | Mundo temporal | Aprobado; 19/19 y JAR reproducible | `docs/releases/1.0.0-validation.md` |
| 2026-08-25 | `clean releaseCheck` x2 | Trading Cells GameTests | Mundo temporal | Aprobado; 17/17 y JAR reproducible | `docs/releases/1.0.0-validation.md` |
| 2026-08-25 | Servidor dedicado | Trading Cells | `run-server/world` | Alcanza `Done (0.270s)` | `docs/releases/1.0.0-validation.md` |
| 2026-08-25 | Smoke RCON, 1.024 inactivas | Trading Cells | `idle-machines` | Aprobado; cierre controlado | `build/performance/release-1.0.0-idle-smoke` |
