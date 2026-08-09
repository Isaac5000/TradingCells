# Resultados de rendimiento

Mediciones realizadas el 2 de agosto de 2026 con Minecraft 26.2, NeoForge
26.2.0.40-beta y Java 25. Cada escenario de servidor usa tres ejecuciones
calentadas y presenta la mediana.

## Insertador de salidas

La prueba aleatoria compara el nuevo insertador con la simulacion anterior en
250.000 inventarios por ejecucion. Verifica tanto la capacidad como el contenido
final y el orden de llenado de slots.

| Ruta | Mediana |
| --- | ---: |
| Simulacion anterior | 45,806 ms |
| Insertador sin inventario temporal | 26,692 ms |
| Mejora | 41,73 % |

## 1.024 maquinas inactivas

Ambas mediciones usan copias del mismo mundo plantilla, semilla, configuracion y
carga. La carga contiene 128 unidades de cada una de estas maquinas: Cultivo para
Aldeanos, Cultivo para Piglins, Cantera de Aldeanos, Cantera de Piglins,
Convertidor, Granja de Hierro, Criadero de Aldeanos y Criadero de Piglins.

| Metrica | Antes | Despues | Cambio |
| --- | ---: | ---: | ---: |
| MSPT medio | 1,572739 | 1,240755 | -21,11 % |
| MSPT p95 | 2,576717 | 1,561241 | -39,41 % |
| Asignaciones/s | 559.240,533 B | 559.240,533 B | 0 % |
| Escrituras de chunks | 10 | 9 | -10 % |

La CPU de este escenario queda por debajo del 1 % y oscila mas que la diferencia
medida, por lo que se considera diagnostica y no una regresion repetible. El
comparador acepta la optimizacion por superar el 10 % tanto en MSPT medio como en
p95 sin empeorar esas latencias.

## Decisiones descartadas

No se guarda un estado `ACTIVE` en el bloque ni se elimina dinamicamente su ticker.
Tras el retorno temprano, el despacho restante de Trading Cells quedo por debajo
del umbral del 10 % en las muestras JFR. El cambio adicional no tenia una mejora
demostrada y aumentaba el riesgo de incompatibilidad con estados persistentes.

Tampoco se compactan mas paquetes ni se cachean tooltips de REI sin una medicion
que muestre una asignacion relevante. Estas hipotesis quedan fuera del codigo para
evitar cambios sin beneficio reproducible.

## Prueba con el mundo existente

Una copia aislada del mundo de desarrollo alcanzo `Done`, ejecuto una grabacion JFR,
guardo las tres dimensiones y se cerro limpiamente mediante RCON. El mundo original
no se modifico. Esta ejecucion es una prueba de compatibilidad, no una comparacion
de rendimiento antes/despues.

REI 26.2.821 mantiene en servidor dedicado sus avisos de `@OnlyIn` y errores al
intentar cargar `LocalPlayer`; las trazas pertenecen al propio JAR de REI, no a
Trading Cells, y no impidieron cargar ni guardar el mundo.

## Cobertura pendiente manual

Las escenas de 256 maquinas activas o bloqueadas, 2.304 intercambios y 64 maquinas
visibles necesitan mundos con entidades capturadas y una sesion cliente real. Las
herramientas permiten repetirlas con `--workload`, pero no se publican cifras hasta
que se ejecuten con dos copias equivalentes del mismo mundo.
