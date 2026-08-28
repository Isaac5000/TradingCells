# Configuracion

Trading Cells crea su configuracion comun mediante NeoForge. Todos los tiempos
se expresan en ticks; Minecraft ejecuta 20 ticks por segundo cuando el servidor
mantiene su ritmo normal.

| Clave | Predeterminado | Rango | Efecto |
| --- | ---: | ---: | --- |
| `timers.breederTicks` | `6000` | `1-72000` | Duracion compartida de ambos criaderos. |
| `timers.incubatorTicks` | `3000` | `1-72000` | Duracion compartida de ambas incubadoras. |
| `timers.farmerGrowthTicks` | `3000` | `1-72000` | Tiempo base de ambos cultivos sin bonificacion de azada. |
| `timers.ironFarmCycleTicks` | `1200` | `1-72000` | Duracion del ciclo de la Granja de Hierro. |
| `timers.villagerInfiniteTrades` | `true` | booleano | Evita que se agoten ofertas en Trader y Autotrader. |
| `production.farmerDamageHoes` | `true` | booleano | Permite que un ciclo completado desgaste la azada respetando Irrompibilidad y los encantamientos propios del mod. |
| `production.ironFarmMultiplierBonus` | `0` | `0-1024` | Se suma a los multiplicadores base `x1`, `x2` y `x3`. |
| `capturers.durability` | `10` | `1-32767` | Liberaciones máximas compartidas por capturadores normales de aldeanos y piglins. |

Cambiar una opcion no altera IDs ni el formato NBT. Para comparar rendimiento o
reproducir un fallo se debe conservar exactamente el mismo archivo de
configuracion entre ejecuciones.

## Soportes visuales de cultivos

Los datapacks pueden clasificar plantas mediante los tags de items
`#trading_cells:farmer_wall_plants`, `#trading_cells:farmer_water_plants` y
`#trading_cells:farmer_ceiling_plants`. Los soportes de pared y techo usan roble
por defecto; las excepciones se amplian con
`#trading_cells:farmer_jungle_support`,
`#trading_cells:farmer_rooted_dirt_support` y
`#trading_cells:farmer_pale_moss_support`.

Si el bloque de una planta añadida por otro mod contiene un fluido en su estado
predeterminado, el Cultivo la reconoce automaticamente como acuatica. El medio
se representa de forma translucida con el cristal tenido vanilla mas cercano a
su color; agua y lava tienen perfiles explicitos. Si no puede inferirse un
fluido para una planta incluida manualmente en el tag acuatico, se usa agua.
