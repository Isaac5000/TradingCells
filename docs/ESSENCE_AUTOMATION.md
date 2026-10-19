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
antiguos. Conservar las dependencias actuales de `gradle.properties`; no restaurar
versiones antiguas desde las evidencias historicas de este documento.
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
Recarga manual de 24 ticks: acercar desde la mano opuesta y encajar vial superior,
33 poses reflejadas por mano. Brazos de tercera persona coordinados con el vial;
agarre centrado en el mango sin modificar la transformacion de primera persona.
Los cambios de carga/datos no activan reequipado ni ocultan el arma.
Consume un vial al terminar, no al cancelar. Estado TradingCellsSyringeLoaded.
Extraccion de 40/80/120/160 ticks (2/4/6/8 segundos, tiers I/II/III/IV): brazo
levantado, avance fijo de 6 ticks (0,3 s), seguido de llenado gradual con el color
del tier durante el tiempo restante. Solo el llenado se ralentiza entre tiers;
25 poses por tier. Objetivo vivo, alcance y linea de vision comprobados de nuevo
al terminar. Cancelacion, objetivo perdido o serializacion fallida no consumen.
Una carga y un uso por extraccion, durabilidad 256, sin cooldown posterior.
Recarga disponible inmediatamente; no se puede extraer otra vez sin cargar vial.
Excepciones de creativo conservadas. Inventario lleno entrega resultado una vez
al mundo; no modifica salud, UUID ni loot de la entidad original.

Receta de jeringa: `III/ LB/  B`; I lingote de hierro, L lapislazuli y B hormigon
negro. Capacidad usa `capacity_<material>_upgrade`; los cinco IDs antiguos
`mob_farm_capacity_<material>_upgrade` permanecen como alias de registro.

## Estabilizador y mesa

Estabilizador portatil, tres entradas y tres salidas: Core, vial recuperado y
recipientes vanilla. 100 ticks, cero XP. Amatista I/II/III/IV: 1/2/4/8;
dos reactivos: redstone/glowstone/ender pearl/dragon breath.
Devuelve tambien dos botellas al usar aliento. Copia integra de datos al Core.
Si falta espacio en cualquier salida, no consume.
Entradas relativas al frontal: vial izquierda, amatista arriba, reactivo derecha.
Automatizacion acepta solo lo pendiente de una ejecucion, segun el vial actual.
Jade muestra progreso y segundos mediante el diagnostico comun, con barra cian
igual que ambas granjas de entidades. REI omite la nota de ticks y coste nulo.

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
Se puede bloquear la receta valida actual,
aunque falten ingredientes o XP. Cambiar receta exige desbloquear.
Receta borrada sigue bloqueada/invalida: no acepta ingredientes ni sustituye
otra. Desbloquear conserva entradas, resultados y XP.
Plantilla fantasma en ranuras vacias y salida tras agotar la receta bloqueada.
El libro propio y sus categorias se retiraron el 2026-10-11; REI sigue disponible.

Entradas superiores/laterales y salidas fisicas usan reglas comunes.
Cada posicion valida Ingredient, componentes y restricciones; cantidades aparte.
Ingredientes repetidos conservan posiciones. Cuota para una ejecucion;
lotes manuales existentes no se reducen.
Solo receta bloqueada se autocraftea, maximo una vez por tick.
Preparar consumo, XP, resultado y restos antes de confirmar; salida completa
obligatoria. Nunca fabricar dentro de una transaccion de insercion.
Modo libre conserva fabricacion manual y transferencias desde REI.
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
Sin cambios en FakePlayer, pendingLoot, Warrior Touch o multiplicadores x32.

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

## Generador de criaturas

La Mesa de generacion de criaturas (`creature_generator`) consume un nucleo
Entity Essence clasificado y un huevo de `#trading_cells:arcane_infusion_eggs`.
El tag existente incluye `#minecraft:eggs`, huevo de tortuga y huevo de sniffer;
los datapacks pueden ampliarlo. No acepta bases ni requiere un huevo generador.
Se fabrica al recoger la salida o
al activar la fabricacion automatica; comparte inventario lateral y cuenta XP
con Essence Workbench sin cambiar sus indices ni datos existentes.
El coste XP es el de `EssenceTier.modelExperience()` (1000/4000/16000/64000).
La lista server-side `creatureGenerator.blacklist` bloquea por defecto
`minecraft:wither` y `minecraft:ender_dragon` antes de consumir entradas. Se
retiraron las recetas activas del Infusor; sus JSON se conservan en
`docs/archive/arcane_infusions/`, salvo Wither y Ender Dragon, eliminados.

La salida usa el Spawn Egg registrado para el tipo. Cuando no existe, el unico
`generic_spawn_egg` guarda `minecraft:entity_data` con el tipo registrado y sin
NBT adicional de entidad. Guarda ademas solo `TradingCellsEssence.Tier` en
`custom_data`: verde I, cian II, violeta III y dorado IV, usando la paleta de las
esencias. Huevos genericos antiguos sin tier se ven como I. REI muestra el huevo
del tier de cada receta; los huevos registrados conservan su aspecto original.
La colocacion usa la ruta vanilla de SpawnEggItem, tambien para
entidades externas. No se copian equipo, pasajeros ni estado arbitrario del
nucleo. Tipos ausentes o esencias invalidas permanecen recuperables sin consumo.

La lista negra se vuelve a consultar antes de confirmar, incluso si una vista
previa ya estaba visible. No bloquea captura, clasificacion ni simulacion. XP,
entradas, salida fisica y modo automatico se conservan al guardar/romper/recolocar.
Las bases que quedaran guardadas de la version anterior se pueden retirar, pero
no sirven para fabricar huevos. El nucleo entra por la izquierda y el huevo por
la derecha; insercion automatica limitada a una unidad pendiente para cada receta.
REI presenta las cuatro combinaciones de tier y su coste; el huevo generico es
un simbolo de salida, no un catalogo cerrado de criaturas.

Validacion 2026-10-04: cuatro GameTests nuevos correctos (tiers, blacklist,
fallback externo y persistencia/rollback); `check` correcto. Clics de XP y
fabricacion comprobados en OpenGL con REI y Vulkan sin REI a 1280x720, escala
automatica, en mundos desechables. Evidencia local `artifacts/creature-generator-*`.
El primer `releaseCheck` detecto el recuento creativo antiguo y un fallo separado
de duplicacion de cofres en logistica. Ambos corregidos; cierre global 2026-10-05:
`check releaseCheck` correcto, 155 GameTests. Sin modificar GitHub.

Correccion de uso 2026-10-08: el usuario aclara que faltaba el huevo del tag.
La implementacion anterior habia usado bases por error. Corregidos filtro del
menu, automatizacion, consumo y REI; los costes siguen siendo 1000/4000/16000/64000
XP segun el nucleo. La fixture `creature-generator` ahora empieza vacia, abre por
interaccion, introduce nucleo/huevo desde el inventario y deposita XP por el menu.
GameTests cubren huevos de gallina/tortuga/sniffer, rechazo de bases, consumo de
una sola unidad de una pila, XP I-IV, blacklist, salida llena y rollback lateral.
Modelo propio de pedestal incubador, cuatro PNG 16x16 y hitbox correspondiente;
no usa texturas de la mesa de esencias. Fuentes y regeneracion en
`tools/assets/creature_generator/README.md`.

Cierre 2026-10-09: `check releaseCheck` correcto, 156 GameTests. Comprobados
apertura, insercion manual, deposito/retirada de XP y recogida de huevo en OpenGL
con REI y Vulkan sin REI; modelo colocado y receta REI inspeccionados.
Evidencias locales: `artifacts/creature-generator-eggs-opengl`,
`artifacts/creature-generator-eggs-vulkan` y `artifacts/creature-generator-eggs-final.log`.
Sin modificaciones del Infusor Arcano ni operaciones en GitHub en esta correccion.
