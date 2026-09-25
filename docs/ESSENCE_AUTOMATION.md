# Esencias y automatizacion

Plan aceptado: 2026-09-23. Revisiones aplicadas: 2026-09-24/25.
Trabajo secuencial, sin subagentes. Este documento es el punto de reanudacion.

## Hitos

- [x] H1: tiers, Threat Score, tags y migracion compatible.
- [x] H2: viales, estabilizador, jeringa 3D, recarga y extraccion animadas.
- [x] H3: XP comun, salida tipo crafting y modo automatico de la mesa.
- [x] H4: bloqueo del infusor, receta fantasma, cuotas y autocraft atomico.
- [x] H5: recetas revisadas, interfaces, traducciones, texturas y REI ordenado.
- [x] H6: check, releaseCheck y matriz OpenGL/Vulkan con/sin REI aprobados.

H1-H6 pueden retirarse de planes futuros: no volver a implementarlos.
No recuperar las granjas retiradas por el usuario ni los cinco PNG genericos
antiguos. Conservar los cambios de dependencias a NeoForge 26.2.0.88.
Nombres de mejoras: resueltos por el usuario el 2026-09-25. No rehacer ni
sobrescribir sus cambios de nombres/traducciones.

## Clasificacion y migracion

Perfil puro y versionado, separado del adaptador de atributos de Minecraft:

Score v1 = 10 log2(1+HP/20) + 6 log2(1+armor/5)
+ 4 log2(1+toughness/2) + 8 log2(1+attack/4)
+ 8 knockbackResistance + 4 log2(1+baseXP/5).

Modificadores separados: hostil +6, ataque a distancia +4, creeper cargado +25.
Umbrales II/III/IV: 20/45/75. Atributos negativos/no finitos/ausentes aportan cero;
entradas finitas limitadas a 1e12, resistencia a [0,1]. Se distinguen 20, 100,
500, 5000 y 100000 HP. XP base se lee de Mob.xpReward, sin loot, muerte ni azar.

Tags: essence_tier_1, essence_tier_2, essence_tier_3, essence_tier_4,
essence_elite y essence_boss. Entre overrides gana el mayor; cualquier override
explicito prevalece sobre Elite/Boss. Sin override, Elite impone minimo III y
Boss IV. high_level_essence sigue incluido en Elite; Wither y Dragon son Boss.

Se conservan TradingCellsEssence, Type y State; se anaden Tier, ThreatScore y
ClassificationVersion=1. No recalcular por ciclo. Migracion una vez en servidor,
desde el estado guardado; HighLevel=true impone minimo III salvo override.
Entidad ausente o estado invalido: objeto recuperable e inactivo, datos intactos.
Snapshot saneado y maximo 65536 bytes; fallos detectados antes del consumo.
IDs conservados: essence_extractor, entity_essence y entity_module.
Esencias antiguas pasan a Core; modelos antiguos no requieren refabricacion.

## Extraccion

Jeringa tipo pistola con cuerpo cerrado 3D, empunadura a 90 grados y aguja hacia
fuera. Modelos de ambas manos, inventario y suelo; no extrusion de sprite 2D.
Recarga manual de 24 ticks: levantar/alinear/encajar vial superior, 33 poses.
Consume un vial al terminar, no al cancelar. Estado TradingCellsSyringeLoaded.
Extraccion de 24 ticks: avance del arma y llenado gradual con el color del tier,
25 poses por tier. Objetivo vivo, alcance y linea de vision comprobados de nuevo
al terminar. Cancelacion, objetivo perdido o serializacion fallida no consumen.
Una carga y un uso por extraccion, durabilidad 256, cooldown posterior 40 ticks.
Excepciones de creativo conservadas. Inventario lleno entrega resultado una vez
al mundo; no modifica salud, UUID ni loot de la entidad original.

## Estabilizador y mesa

Estabilizador portatil, tres entradas y tres salidas: Core, vial recuperado y
recipientes vanilla. 100 ticks, cero XP. Amatista I/II/III/IV: 1/2/4/8;
dos reactivos: redstone/glowstone/ender pearl/dragon breath.
Devuelve tambien dos botellas al usar aliento. Copia integra de datos al Core.
Si falta espacio en cualquier salida, no consume.
Entradas relativas al frontal: vial izquierda, amatista arriba, reactivo derecha.
Automatizacion acepta solo lo pendiente de una ejecucion, segun el vial actual.

Mesa: Core y base del mismo tier, costes 1000/4000/16000/64000 XP interna.
Vista previa se fabrica al recoger la salida, sin boton Create Model.
Sin base fantasma ni Tier I predeterminado. Inventario lleno no consume al
hacer shift-click. Materiales antiguos recuperables en ranura adicional.
Modo AutomaticSynthesis persistente: fabrica automaticamente con ingredientes,
XP y salida disponible. Por tuberias: Core izquierda, base derecha.
Geometria de bases negra y posiciones de mano conservadas.

XP compartida con infusor mediante MachineExperienceAccount.
Capacidad Integer.MAX_VALUE, aritmetica protegida, fluido/XP 1:1.
Modo sincronizado/persistente Solo receta activa (predeterminado) o Llenar
almacenamiento. Cuota automatica independiente de capacidad fisica: cambiar
receta no recorta XP. Transferencias manuales no tienen cuota de receta.
Capabilities por las seis caras con rollback. GUI: almacen/jugador, XP/niveles,
campo de niveles (vacio=Todo), Guardar/Retirar y selector independiente.

## Infusor

LockedRecipe guarda ID; sincroniza estado, resultado y plantilla completa.
Se puede bloquear receta valida actual o seleccionada mediante el recetario,
aunque falten ingredientes o XP. Cambiar receta exige desbloquear.
Receta borrada sigue bloqueada/invalida: no acepta ingredientes ni sustituye
otra. Desbloquear conserva entradas, resultados y XP.
Plantilla fantasma en ranuras vacias y salida tras agotar la receta bloqueada.
Transferencias del recetario no sustituyen el bloqueo.

Entradas superiores/laterales y salidas fisicas usan reglas comunes.
Cada posicion valida Ingredient, componentes y restricciones; cantidades aparte.
Ingredientes repetidos conservan posiciones. Cuota para una ejecucion;
lotes manuales existentes no se reducen.
Solo receta bloqueada se autocraftea, maximo una vez por tick.
Preparar consumo, XP, resultado y restos antes de confirmar; salida completa
obligatoria. Nunca fabricar dentro de una transaccion de insercion.
Modo libre conserva fabricacion manual y recetario por lotes.
XP Mode no depende del candado. No se introduce redstone.

Botella de vidrio + 11 XP es shapeless: cualquier casilla, sin otros ingredientes.
El soporte shapeless actual se limita explicitamente a un unico ingrediente;
no promete resolver recetas shapeless arbitrarias.

## Recetas y recursos

Las revisiones mas recientes sustituyen las recetas del plan inicial:

- Jeringa diagonal: amatista arriba derecha, panel de vidrio central, hierro abajo izquierda.
- Cuatro viales: columna A/G/G (amatista, vidrio, vidrio).
- Estabilizador: LGL/BDB/LBL, L lapislazuli, G vidrio, B hormigon negro, D bloque de diamante.
- Mesa: LBL/BCB/BSB, L lapislazuli, B hormigon negro, C almacen de XP, S storm shard.
- Granja en infusor: LBL/BCB/LSL, mismos simbolos, 50000 XP.
  Ambos cuarzos inferiores se sustituyeron por lapislazuli.
- Cuatro bases: GBG/BSB/GBG, B hormigon negro, S storm shard; G es hormigon
  verde/lapislazuli/diamante/lingote de netherita segun tier I/II/III/IV.
- Mejoras progresivas: bloques del material; primera centrada en espada de diamante,
  siguientes en mejora anterior; esquinas reloj/cofre segun velocidad/capacidad.
  Diamante usa popped chorus fruit; netherita usa smithing con bloque y plantilla.

Contrato actual: 164 recetas, fingerprint
9D2E688720183BADC4B0781A6B382A375BCE549733BB7561B46B6E2E581FDAF1.
16 payload IDs activos, 74 claves NBT, schema 1, network 2.
Contratos distinguen payloads/clases retirados de los activos; no restaurarlos.

Perimetros animados: I #55E66A/#B9FFC2; II #35CFFF/#B7F3FF;
III #B05CFF/#E0C2FF; IV #FFD34E/#FFF2A6. Jeringa/viales/Core/estabilizador
tienen recursos propios. Fuentes historicas en tools/assets/essence.
Generador reproduce 211 recursos. GUI y REI del infusor con End Stone.
Flechas convencionales; estabilizador integra progreso y acceso REI en flecha.
REI muestra costes y tiers I-IV, sin controlar bloqueo. Orden aplicado solo
a copias de las vistas de esencias, no al registro compartido de REI.
Interfaces es_es/en_us y variante Creeper cargado disponibles. Los nombres
de mejoras quedan a cargo del usuario, sin cambios adicionales del agente.

## Limpieza y mejoras genericas

Usuario retiro las granjas por familia y sus dependencias. Botin nativo sigue
ejecutandose desde la tabla de la entidad real mediante MobFarmLootTables.
MobFarmLootPreview conserva probabilidades sin tirar loot ni avanzar azar del
mundo. Tablas desconocidas muestran probabilidad desconocida, no cero inventado.
MobFarmEquipmentLoot conserva perfiles de equipo, Looting, desgaste, armas
excluyentes y recompensas ominosas dentro del sistema general.
Sin cambios en FakePlayer, pendingLoot, Warrior's Touch o multiplicadores x32.

Cinco nuevos marcos generales en tools/assets/upgrade_bases/frames y
textures/item/upgrades/generic: copia de quarry con emblema central retirado.
Mismo tamano, marco, alfa y paleta; no restauran los PNG borrados.
25 variantes existentes conservadas pixel a pixel. El generador normal usa
estas fuentes independientes, no sus salidas ni originales eliminados.

## Validacion vigente

2026-09-25, NeoForge 26.2.0.88: check y releaseCheck aprobados, proceso exit 0.
118/118 GameTests, 30 pruebas Python de recursos, 27 de recetas, dominio puro,
arquitectura, portabilidad, JAR, 1172 JSON y 185 PNG.
El total anterior era mayor por las pruebas de granjas eliminadas por el usuario.

Incluye clasificacion/extremos, tres criaturas fuera del namespace del mod,
overrides, migracion, entidad ausente, extraccion/cancelacion/serializacion,
cuatro estabilizaciones, recipientes, tiers, XP int max/rollback, ranuras
repetidas, componentes, restos, salidas llenas, tolvas reales, dos botellas
sucesivas, recarga real de datapacks, clic de salida y automatizacion por caras.
Regresiones de limpieza: probabilidades antes del primer ciclo, loot imposible,
tablas enlazadas, perfiles de equipo, recompensas ominosas y tuberia horizontal.

OpenGL + REI + es_es + mano izquierda:
artifacts/essence-automation-3d-rei-order-20260925
Clics/paquetes reales, recarga, extraccion y orden de las cuatro recetas aprobados.
Captura 03b-stabilizer-recipes y fases 01-13; jeringa solida, vial llenandose,
XP/candado/salida comprobados.

Matriz completa sobre NeoForge 0.88, cuatro procesos exit 0:
- OpenGL + REI + es_es + izquierda: essence-automation-3d-rei-order-20260925.
- Vulkan + REI + en_us + derecha: essence-automation-vulkan-rei-20260925.
- OpenGL sin REI + en_us + derecha: essence-automation-opengl-no-rei-20260925.
- Vulkan sin REI + es_es + izquierda: essence-automation-vulkan-no-rei-20260925.
Todos bajo artifacts/, 21 fases de clics/paquetes reales completadas.
Capturas revisadas sin solapes; ambas manos y modelo cerrado comprobados.
Llenado Vulkan: region interior del vial (1000,449)-(1075,585), 3607 pixeles
de liquido en fase intermedia y 8157 al completar; color III morado correcto.
El orden I-IV se verifica en la vista real de REI despues de la sincronizacion.
Evidencia anterior de NeoForge 0.57 bajo essence-automation-3d-*-20260924.

JAR local validado: build/libs/trading_cells-1.0.0.jar.

## Limites

Pruebas cubren entidades vanilla y tres tipos externos de prueba, no todos los
mods posibles. Loot de condiciones/funciones no soportadas conserva ejecucion
nativa y muestra probabilidad desconocida. Capturas graficas son evidencia
local a 1280x720, no un benchmark de rendimiento ni aprobacion universal de drivers.
No se ha publicado, hecho commit ni subido esta revision a GitHub.
