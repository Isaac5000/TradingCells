# Inventario de criaturas y granjas

Este inventario usa `EntityTypes` de Minecraft 26.2 como referencia. Su objetivo
es evitar volver a clasificar todas las criaturas al ampliar Trading Cells. Los
tags siguen siendo la fuente de verdad en ejecucion: una criatura de otro mod
entra automaticamente cuando pertenece al tag de su familia, aunque no tenga
botin propio.

## Familias implementadas

| Granja | Objetivos vanilla | Tag dinamico | Coste |
| --- | --- | --- | ---: |
| Esqueletos | Skeleton, Stray, Wither Skeleton, Bogged, Parched y Skeleton Horse | `#minecraft:skeletons` | 50.000 XP |
| Zombis | Zombie, Zombie Villager, Husk, Drowned, Zombified Piglin, Zoglin, Zombie Horse, Camel Husk y Zombie Nautilus | `#minecraft:zombies` | 50.000 XP |
| Saqueadores | Pillager, Evoker, Ravager, Vindicator, Illusioner, Witch y Vex | `#trading_cells:raider_farm_targets` | 100.000 XP |
| Creepers | Creeper y variante sintetica cargada | `#trading_cells:creeper_farm_targets` | 25.000 XP |
| Artropodos | Spider, Cave Spider, Silverfish y Endermite | `#trading_cells:arthropod_farm_targets` | 35.000 XP |
| Slimes | Slime, Magma Cube y Sulfur Cube | `#trading_cells:slime_farm_targets` | 45.000 XP |
| Guardianes | Guardian y Elder Guardian | `#trading_cells:guardian_farm_targets` | 75.000 XP |
| Piglins | Piglin y Piglin Brute | `#trading_cells:piglin_farm_targets` | 70.000 XP |
| Blazes | Blaze | `#trading_cells:blaze_farm_targets` | 55.000 XP |
| Ghasts | Ghast y Happy Ghast | `#trading_cells:ghast_farm_targets` | 65.000 XP |
| Endermen | Enderman | `#trading_cells:enderman_farm_targets` | 60.000 XP |
| Shulkers | Shulker | `#trading_cells:shulker_farm_targets` | 90.000 XP |
| Breezes | Breeze | `#trading_cells:breeze_farm_targets` | 80.000 XP |
| Phantoms | Phantom | `#trading_cells:phantom_farm_targets` | 40.000 XP |
| Animales | Cow, Mooshroom, Sheep, Pig, Chicken, Rabbit y Goat | `#trading_cells:livestock_farm_targets` | 20.000 XP |
| Peces | Cod, Salmon, Tropical Fish y Pufferfish | `#trading_cells:fish_farm_targets` | 20.000 XP |
| Acuaticas | Squid, Glow Squid, Dolphin y Nautilus | `#trading_cells:aquatic_farm_targets` | 30.000 XP |
| Monturas | Horse, Donkey, Mule, Camel, Llama y Trader Llama | `#trading_cells:mount_farm_targets` | 30.000 XP |
| Anfibios | Axolotl, Frog, Tadpole y Turtle | `#trading_cells:amphibian_farm_targets` | 25.000 XP |
| Abejas | Bee | `#trading_cells:bee_farm_targets` | 25.000 XP |
| Creakings | Creaking | `#trading_cells:creaking_farm_targets` | 85.000 XP |

La Granja de Hierro cubre al Iron Golem mediante su mecanica propia. El Creeper
cargado no es un `EntityType` distinto y por eso sigue siendo una variante de
codigo. Sulfur Cube y Happy Ghast permanecen en sus familias aunque hoy no
aporten un botin util: un datapack o mod puede darselo en el futuro.

En total hay cuatro familias historicas y diecisiete configurables. Los objetivos
sin botin fisico completan ciclos de XP y siguen disponibles para recompensas de
datapacks o mods.

## Candidatos para futuras granjas

| Prioridad | Familia propuesta | Criaturas | Motivo o separacion |
| --- | --- | --- | --- |
| Baja | Sniffers | Sniffer | Granja individual orientada a excavacion, no a muerte directa. |
| Baja | Striders | Strider | Granja individual del Nether con entorno y botin muy limitados. |
| Baja | Fauna salvaje | Armadillo, Fox, Panda, Polar Bear y Ocelot | Categoria posible, pero heterogenea y con poco botin directo. |

Cada nueva familia debe usar un tag propio, snapshots inmutables reconstruidos
solo en recargas y descriptores `mob_farm_target` de esquema 1. Una criatura sin
botin puede estar seleccionable; simplemente completa ciclos de XP hasta que un
datapack le aporte una tabla util.

## Excluidas por ahora

| Grupo | Criaturas | Razon |
| --- | --- | --- |
| Caso ambiguo | Hoglin | Es hostil y a la vez animal de granja; se mantiene fuera por decision expresa. |
| Jefes y progresion especial | Ender Dragon, Wither y Warden | Sus recompensas y activacion no deben reducirse a un ciclo normal de granja. |
| Aldeanos y comercio | Villager y Wandering Trader | Ya pertenecen a capturadores, criaderos, traders y conversion. |
| Companeros o domesticables | Allay, Cat, Wolf y Parrot | Sin propuesta de produccion que justifique una granja de muerte. |
| Constructos | Copper Golem y Snow Golem | Utilidad de mundo sin botin normal; Iron Golem ya tiene granja propia. |
| Ambientales o no disponibles | Bat y Giant | Sin botin util; Giant no forma parte del juego normal. |

Armor Stand, Mannequin, Player, proyectiles, vehiculos, displays, objetos y las
demas entidades tecnicas no son criaturas candidatas y no forman parte de este
inventario funcional.

## Contrato para una granja nueva

- Receta de 3x3: Almacen de XP en el centro; Spawner, bloque base y Bloque de
  hierro en la fila inferior; huevos o materiales tematicos en los otros huecos.
- Coste de granja multiplo de 5.000 XP y nunca superior a 100.000 XP.
- Receta de cada Spawn Egg existente, con coste multiplo de 10 y maximo de 550
  XP, equivalente a 20 niveles completos.
- Selector, botin real cargado, filtros, XP, pausa, persistencia, caras de
  automatizacion, REI, Jade, modelos, particulas y GameTests.
- Si Minecraft no proporciona un Spawn Egg para una criatura, no se inventa un
  item alternativo sin una decision de diseno independiente.
