# Granja de Zombis

La Granja de Zombis es una máquina portátil independiente basada en la geometría de la Granja de Esqueletos. Conserva aldeano, espada, dieciocho salidas, objetivo, filtros, progreso, botín pendiente, pausa y XP al romperse y recolocarse.

## Objetivos

- Zombi, aldeano zombi y zombi momificado: carne podrida, botín raro, armas y cabeza con Decapitación.
- Ahogado: carne podrida, tridente o caña, cobre, concha de nautilo y cabeza con Decapitación.
- Piglin zombificado: carne podrida, pepitas, lingotes, espada de oro y cabeza de piglin con Decapitación.
- Zoglin: carne podrida y añadidos de tablas de botín; no se inventa una cabeza inexistente.

El selector conserva las seis variantes fijas y añade automáticamente las entidades monstruosas externas incluidas en `#minecraft:zombies`. Cada recompensa puede activarse por separado. Los objetos desconocidos de las tablas cargadas aparecen como filtros individuales con su propio nombre e icono; no existe una casilla genérica de `Botín de mods`.

## Funcionamiento

- Golpeo V aporta la máxima reducción vanilla contra no muertos; Filo y efectos de daño compatibles también aceleran con menor eficacia.
- Botín modifica cantidades y probabilidades a través de la tabla de botín real de la entidad.
- Filo Arrasador añade una baja simulada por nivel.
- Irrompibilidad usa el desgaste vanilla y Toque del Guerrero evita completamente el daño de la espada.
- Cada baja simulada añade cinco puntos de XP, incluso si no hay recompensas seleccionadas.
- Una salida completamente llena pausa ciclos que podrían generar objetos; si no hay ningún botín seleccionado, la generación de XP continúa.

La ejecución usa las tablas de botín cargadas con un jugador simulado que porta la espada instalada. Esto conserva funciones vanilla y permite incorporar añadidos de datapacks, reemplazos de tablas y modificadores compatibles. Si una tabla externa falla, se descarta el lote parcial y se usa un fallback vanilla sin bloquear la máquina.

## Ahogados

El equipo se simula por separado porque no forma parte de la tabla de botín: puede producir tridentes o cañas de pescar con sus probabilidades vanilla. El cobre usa `11 % + 2 %` por nivel de Botín y la concha de nautilo mantiene su aparición base del `3 %`.

## Piglins zombificados y zoglins

El piglin zombificado reproduce carne y pepitas de `0-1`, lingote raro de `2,5 %` y la probabilidad de soltar su espada de oro; Botín aplica la curva vanilla. El zoglin produce `1-3` de carne podrida antes de Botín.

El huevo de piglin zombificado forma parte de la receta 3x3 de la granja. El zoglin tiene su propia infusión y aparece en el selector, pero no participa en la receta del bloque.

## REI y renderizado

REI crea una vista por objetivo con aldeano, espada, huevo, tiempo y un único slot rotatorio de resultados. La pantalla normal usa una copia propia del diseño, de modo que sus cambios no alteran la Granja de Esqueletos. En el mundo se renderizan un armero, un spawner y la entidad seleccionada sobre una base de musgo pálido.

El catálogo usa `#minecraft:zombies` para descubrir especies y las entradas de objetos o tags de sus tablas cargadas para construir los filtros. Los fallos se aíslan por tabla o entidad y la lista fija sigue disponible como respaldo. El botín añadido únicamente por código o por modificadores globales opacos no puede enumerarse por adelantado, aunque la ejecución de la tabla real mantiene la compatibilidad cuando ese sistema participa en ella.
