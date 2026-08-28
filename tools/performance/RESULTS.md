# Resultados de rendimiento

Resultados locales conservados en `build/performance`. Los artefactos pesados
estan ignorados por Git; este archivo registra las medianas y las decisiones.

## 405 Granjas de Zombis activas

Medicion del 28 de agosto de 2026 con una copia fija del mundo `Test`, 405
Granjas de Zombis activas en un volumen `9x9x5`, camara fija, REI, resolucion
`1920x1080`, 15 s de calentamiento y 30 s de captura. Cada cifra es la mediana
de tres arranques limpios. Los valores exactos se conservan en
`results/2026-08-28-zombie-farm-405.csv`.

### Cliente Vulkan

| Metrica | Antes | Despues | Cambio |
| --- | ---: | ---: | ---: |
| FPS | 17,965 | 45,338 | +152,37 % |
| Fotograma medio | 54,024 ms | 21,141 ms | -60,87 % |
| Fotograma p95 | 60,479 ms | 23,488 ms | -61,16 % |
| Fotograma p99 | 64,782 ms | 28,792 ms | -55,56 % |
| Asignaciones | 1.764,8 MB/s | 244,8 MB/s | -86,13 % |
| Memoria residente | 2.041,3 MB | 1.859,7 MB | -8,90 % |
| Paquetes/30 s | 12.652 | 3.643 | -71,21 % |
| Bytes/30 s | 14.477.657 | 1.044.092 | -92,79 % |

El mismo candidato en OpenGL dio 49,551 FPS, 19,646 ms medios, 21,745 ms p95
y 255,5 MB/s de asignaciones. Las capturas de ambos backends conservan la misma
geometria, entidades, iluminacion y composicion.

Se conservaron dos cambios independientes:

- Las entidades de previsualizacion viven ahora en caches debiles ligadas a la
  Block Entity. El estado visual sigue extrayendose y la luz sigue leyendose en
  cada fotograma cuando corresponde.
- El progreso de las granjas usa su `ContainerData`; ya no envia el NBT completo
  de inventarios, filtros y botin cada segundo. Los cambios visibles, slots y
  ciclos completos siguen sincronizandose en el mismo instante.

JFR situa ahora el coste principal en la emision real de vertices de los modelos
(`BufferBuilder` y subida de buffers). No se aplico LOD, distancia adicional,
reduccion de modelos ni una ruta especifica de backend porque alterarian el
contenido visible o aumentarian el riesgo sin equivalencia demostrada.

### Servidor dedicado

La misma plantilla, con sus chunks forzados antes del calentamiento, obtuvo
2,474609 MSPT medios, 3,640878 MSPT p95 y 20,013 TPS. El procesamiento propio de
`ZombieFarmBlockEntity` represento alrededor del 1,33 % de las muestras JFR; no
se introdujo un refactor mecanico de servidor porque no alcanzaria el umbral y
arriesgaria tiempos o tiradas aleatorias.

## Matrices mixtas de 405 maquinas

Medicion del 28 de agosto de 2026 con copias verificadas del mundo `Test`, una
matriz `9x9x5`, camara fija, REI, `1920x1080`, 15 s de calentamiento y 30 s de
captura. Cada cifra es la mediana de tres arranques. Los datos completos estan en
`results/2026-08-28-machine-matrices-405.csv`.

Las plantillas alternan las cuatro familias de granjas de entidades, ocho tipos
de maquina de Aldeanos o seis tipos de maquina de Piglins. Se verificaron sus
Block Entities antes de medir; las mediciones de servidor fuerzan todos los
chunks de la matriz.

### Cliente

| Familia | Backend | FPS | Media | p95 | Asignaciones |
| --- | --- | ---: | ---: | ---: | ---: |
| Granjas de entidades | Vulkan | 59,282 | 16,099 ms | 18,260 ms | 268,2 MB/s |
| Granjas de entidades | OpenGL | 65,150 | 14,909 ms | 18,751 ms | 341,3 MB/s |
| Maquinas de Aldeanos | Vulkan | 64,198 | 14,865 ms | 17,324 ms | 400,7 MB/s |
| Maquinas de Aldeanos | OpenGL | 72,022 | 13,483 ms | 16,680 ms | 393,0 MB/s |
| Maquinas de Piglins | Vulkan | 47,306 | 20,326 ms | 22,673 ms | 634,8 MB/s |
| Maquinas de Piglins | OpenGL | 43,514 | 22,316 ms | 27,245 ms | 361,7 MB/s |

Las capturas OpenGL y Vulkan conservan la misma composicion y todos los modelos.
Las Granjas de Zombis, Esqueletos, Creepers y Saqueadores usan la misma estrategia
de cache debil de entidad y estado visual; no queda una optimizacion exclusiva de
la Granja de Zombis.

Una pasada diagnostica de una ejecucion por tipo dio 321,9 FPS para el Trocador
normal, 245,0 para el Trocador de Netherita, 292,7 para el Cultivo y 324,8 para
la Cantera. No se usa como resultado estadistico. JFR situa el coste de la matriz
mixta de Piglins en vertices, modelos de bloque y ordenacion de capas distintas,
no en una unica Block Entity ni en la logica del servidor.

### Servidor dedicado

| Familia | MSPT medio | MSPT p95 | TPS | Asignaciones |
| --- | ---: | ---: | ---: | ---: |
| Granjas de entidades | 1,933302 | 2,805669 | 20,019987 | 6,85 MB/s |
| Maquinas de Aldeanos | 1,387473 | 1,916112 | 20,026658 | 3,22 MB/s |
| Maquinas de Piglins | 1,421500 | 2,305432 | 20,019987 | 3,36 MB/s |

Esto confirma que la diferencia de Piglins es grafica: su coste de servidor queda
junto al de Aldeanos y muy lejos del limite de 50 ms por tick.

### Oclusion descartada

Se probo omitir previsualizaciones internas solo cuando centro y ocho esquinas
quedaban detras de bloques opacos. En la matriz encerrada en piedra redujo las
asignaciones un 26,12 %, pero solo mejoro los FPS un 1,35 %. En Piglins visibles
empeoro los FPS un 9,90 % y el p95 un 14,59 %.

Una segunda variante reutilizo la visibilidad dentro de cada tick. Redujo las
asignaciones un 42,64 %, pero aun empeoro los FPS un 5,21 % y el p95 un 7,39 %.
Ambas variantes fueron retiradas por superar el limite de regresion. No queda
codigo de trazado de visibilidad ni ramas especificas de OpenGL/Vulkan.

## Estabilizacion 1.0.0

Esta pasada no modifica ninguna ruta de produccion medida. Se conservaron las
optimizaciones aceptadas que aparecen debajo y no se reintrodujo ninguna
hipotesis descartada. Las herramientas ahora exigen un manifiesto y la misma
huella de plantilla para baseline y candidato; no se publica una mejora nueva
sin mundos activos, bloqueados, multijugador y cliente comparables.

La referencia previa al nucleo de granjas y al catalogo de datapacks esta en
`baselines/stabilization-1.0.0.json`, con commit, estado del arbol y SHA-256 del
JAR. El refactor se acepta por equivalencia determinista y GameTests; no se le
atribuye una mejora de rendimiento hasta ejecutar la plantilla `mob-farms`.

El candidato posterior esta registrado en
`baselines/stabilization-1.0.0-candidate.json`. Dos ejecuciones consecutivas de
`clean releaseCheck` del 25 de agosto generaron un JAR identico de 1.813.690
bytes con SHA-256
`B4830A7A27F7A53997641012BAD3BD2E319F703A6432E290705D9C70DAF4386D`.

La revalidacion pura de esta candidata produjo:

| Ruta | Ejecuciones/casos | Anterior | Actual | Cambio |
| --- | ---: | ---: | ---: | ---: |
| Insertador de salidas | 3 x 250.000 | 38,247 ms | 22,623 ms | +40,85 % |
| Tooltips perezosos | 5 x 2.000.000 | 32,939 ms | 11,296 ms | +65,71 % |
| Cache candidata Autotrader | 5 x 5.000.000 | 7,458 ms | 7,519 ms | -0,82 % |

La cache del Autotrader sigue retirada. Un smoke test no comparativo de 1.024
maquinas inactivas dio 1,691120 MSPT medio y 1,914873 p95; solo certifica el
ejecutor, la carga y el cierre RCON actuales.

## Cambios aceptados

### Insertador de salidas

Prueba aleatoria de 250.000 inventarios por ejecucion, con equivalencia de
capacidad, contenido y orden de slots.

| Ruta | Mediana |
| --- | ---: |
| Simulacion anterior | 76,037 ms |
| Insertador compartido | 48,255 ms |
| Mejora | 36,54 % |

### Maquinas inactivas

Medicion historica del 2 de agosto de 2026 con 1.024 maquinas, tres ejecuciones
calentadas y el mismo mundo.

| Metrica | Antes | Despues | Cambio |
| --- | ---: | ---: | ---: |
| MSPT medio | 1,572739 | 1,240755 | -21,11 % |
| MSPT p95 | 2,576717 | 1,561241 | -39,41 % |

Se conserva `MachineActivityController` con retornos tempranos. Los contadores
siguen avanzando exactamente por tick cuando una maquina esta activa.

### Tooltips de encantamientos

Medicion del 9 de agosto de 2026, cinco ejecuciones y 250.000 casos aleatorios de
equivalencia por ejecucion. Escenario comun: objeto sin encantamientos por encima
de su limite.

| Ruta | Mediana |
| --- | ---: |
| Mapa creado siempre | 48,247 ms |
| Mapa perezoso | 13,704 ms |
| Mejora | 71,60 % |

Solo se evita crear un `HashMap` vacio. Texto, color y orden del tooltip no cambian.

## Hipotesis retiradas

- Ticker dinamico mediante un estado `ACTIVE`: menos del 10 % tras los retornos
  tempranos y mayor riesgo de diferencias de tick.
- Cache de preparacion del Autotrader: la repeticion final de cinco ejecuciones dio
  12,479 ms frente a 11,605 ms, solo un 7,00 %. Todo el cambio de produccion fue
  retirado.
- Cache de posiciones/contextos en cinco renderizadores: la candidata empeoro la
  mediana de 1,865/3,295 ms (media/p95) a 5,620/17,592 ms. Se restauro exactamente
  el codigo anterior.
- Snapshots compactos de Block Entity, agrupacion de paquetes e intercambio masivo:
  no se conservaron cambios sin una plantilla multijugador comparable.
- Caches adicionales de REI o textos: JFR no mostro una ruta propia con peso
  suficiente en la escena disponible.

## OpenGL y Vulkan

Escena fija de 64 maquinas, 15 s de calentamiento y 5 s de medicion. Son pruebas
funcionales de una ejecucion, no una comparacion estadistica entre backends.

| Backend | Media | p95 | Estado |
| --- | ---: | ---: | --- |
| OpenGL | 1,874 ms | 3,187 ms | 64 maquinas completas |
| Vulkan | 1,161 ms | 1,873 ms | 64 maquinas completas |

Las capturas tienen la misma geometria y composicion. Solo 44 de 921.600 pixeles
difieren mas de dos niveles de canal; la diferencia maxima es 12. REI 26.2.821
carga y cierra correctamente en ambos backends. Sus avisos `@OnlyIn` pertenecen
al propio REI.

## Control vanilla

Cinco ejecuciones OpenGL de 10 s, con mundo y camara fijos:

| Variante | Media | p95 |
| --- | ---: | ---: |
| Sin Trading Cells | 1,285 ms | 2,836 ms |
| Con Trading Cells | 1,353 ms | 2,745 ms |

El resultado es inconcluso: la ruta media varia entre 0,799 y 4,959 ms aun sin el
mod, mientras media y p95 cambian en sentidos opuestos. El comparador rechaza la
estabilidad por +5,29 % en la media; no se atribuye causalidad ni se publica una
mejora. Las cinco parejas de capturas son visualmente iguales dentro de un nivel
de canal.

## Servidor dedicado

La plantilla vanilla y su medicion alcanzaron `Done`, guardaron el mundo y se
cerraron mediante RCON. No aparecen `LocalPlayer`, `ClassNotFoundException` ni
el mod cliente `trading_cells_performance` en el servidor. El mensaje de
generador plano `No key layers in MapLike[{}]` procede de la configuracion
vanilla de la plantilla y no de Trading Cells.
