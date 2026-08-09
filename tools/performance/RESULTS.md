# Resultados de rendimiento

Resultados locales conservados en `build/performance`. Los artefactos pesados
estan ignorados por Git; este archivo registra las medianas y las decisiones.

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
- Caches adicionales de REI, entidades o textos: JFR no mostro una ruta propia con
  peso suficiente en la escena disponible.

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
