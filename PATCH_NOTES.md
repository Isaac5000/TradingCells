# Patch Notes (undécima corrección)

## Menú de Criadero de Piglins
- Sustituida la textura del menú de piglins por la versión corregida manualmente proporcionada por el usuario.
- Esta pasa a ser la referencia exacta para el menú de piglins.

## Menú de Criadero de Aldeanos
- Ajustado el menú de aldeanos para replicar la misma forma base y la misma retícula inferior del inventario usada en el menú de piglins.
- Se conserva el tema visual de aldeanos, pero con la misma estructura simétrica de inventario y layout inferior.

## Coherencia entre menús
- Ambos criaderos comparten ahora la misma base de inventario en la parte inferior, manteniendo una distribución uniforme y simétrica.
- Se mantiene la resolución del menú en 176x222.

# Patch Notes (duodécima corrección)

## Botón para renovar intercambios
- Añadido el botón de renovación al menú propio del Autotrocador de Aldeanos.
- La acción se procesa en el servidor mediante el sistema de botones del menú, sin depender del paquete usado por el Trocador de Aldeanos.
- Solo se permite renovar aldeanos con profesión temporal, POI válido, nivel inicial y sin experiencia laboral, igual que en el Trocador de Aldeanos.
- Tras renovar se actualizan las ofertas guardadas, se selecciona la primera oferta y se reinicia el tiempo de persistencia de intercambios.

## Cultivo para Aldeanos
- El contador y el progreso visible solo se muestran mientras existe un proceso de cultivo activo y con espacio para guardar la cosecha.
- La Eficiencia de la azada reduce ahora la duración máxima real del ciclo; el contador utiliza ese nuevo tiempo en lugar de avanzar a saltos sobre el tiempo original.
- Al cambiar la azada se conserva proporcionalmente el progreso del ciclo.
- Recentrados los slots del aldeano, la azada y la semilla.
- El slot de cultivo admite una sola unidad y evita que las tolvas acumulen más semillas en él.

## 2026-07-12 — Velocidad dinámica del Cultivo para Aldeanos

- El tiempo base de cultivo pasa a 2 minutos (`2400` ticks).
- La duración se reduce proporcionalmente usando la velocidad real del componente `minecraft:tool` de la azada. Esto permite que materiales añadidos por otros mods sean más rápidos automáticamente cuando declaren una velocidad superior.
- Una azada de madera mantiene el tiempo base; las azadas más rápidas reducen la duración del ciclo.
- Eficiencia aporta velocidad adicional, pero su efecto queda limitado al nivel V. Los niveles VI o superiores no reducen más el tiempo.
- El ciclo nunca puede durar menos de 1 segundo (`20` ticks).
- Al cambiar la azada o sus modificadores, el progreso se reescala para mantener el porcentaje completado.

## 2026-07-13 — Reposición de intercambios y nueva curva del Cultivo

### Trocador de Aldeanos y Autotrocador

- Los aldeanos almacenados vuelven a reponer los usos de sus ofertas mediante la lógica vanilla de `Villager.shouldRestock` y `Villager.restock`.
- La comprobación se realiza cada 300 ticks; las propias reglas vanilla siguen limitando cuándo puede reponer y cuántas veces lo hace durante el día.
- Ya no se regeneran ofertas aleatorias para recuperar el stock: se conservan los mismos intercambios y se restablecen sus usos.
- Nueva opción de configuración:

```toml
[trading]
villagerInfiniteTrades = false
```

Al activarla, cualquier oferta usada se restablece inmediatamente y nunca queda agotada, tanto en el comercio manual como en el automático.

### Cultivo para Aldeanos

- El tiempo base sin azada sigue siendo de 2 minutos (`2400` ticks).
- Colocar una azada de madera ya reduce el tiempo respecto a no usar herramienta.
- La reducción por tier utiliza la velocidad real del componente `minecraft:tool`, por lo que admite azadas de mods con velocidades superiores a las vanilla.
- La curva usa rendimientos decrecientes: cada salto de tier mejora el tiempo, pero menos que el anterior.
- Eficiencia continúa limitada a nivel V.
- El efecto de Eficiencia depende del tier: es moderado en madera y aumenta progresivamente en herramientas más rápidas.
- Se conserva el mínimo absoluto de 1 segundo (`20` ticks).

Tiempos orientativos con la configuración predeterminada:

| Herramienta | Sin Eficiencia | Eficiencia V |
|---|---:|---:|
| Sin azada | 120 s | 120 s |
| Madera | 102,6 s | 94,4 s |
| Piedra | 96,8 s | 85,1 s |
| Cobre | 94,6 s | 81,7 s |
| Hierro | 92,7 s | 78,7 s |
| Diamante | 89,6 s | 73,9 s |
| Netherita | 88,2 s | 71,8 s |
| Oro | 84,8 s | 66,6 s |

## Ajustes de intercambios, granja y cultivo

- Trocador y Autotrocador:
  - Los descuentos acumulados por comerciar se guardan permanentemente en el aldeano.
  - Los descuentos de curación se acumulan sin límite interno; el precio efectivo sigue respetando el mínimo de un objeto de Minecraft.
  - El modo de intercambios infinitos está activado por defecto y evita mostrar la flecha de agotado en el Autotrocador.
  - Las ofertas se reponen antes de enviarlas al jugador cuando el modo infinito está activo.
- Autotrocador:
  - Los costes mostrados se recalculan y persisten con los descuentos acumulados.
- Granja de hierro:
  - Ciclo base reducido a 60 segundos.
  - Multiplicadores base cambiados a x2, x4 y x8.
  - Nueva opción `ironFarmMultiplierBonus`; suma el mismo valor a los tres multiplicadores.
- Cultivo para Aldeanos:
  - Tiempo base cambiado a 150 segundos.
  - Curva recalibrada para que una azada de netherita con Eficiencia V tarde aproximadamente 26 segundos.
  - Eficiencia sigue limitada a nivel V y escala más cuanto mejor sea el tier real de la herramienta.
  - Las herramientas de mods por encima de netherita continúan mejorando dinámicamente hasta el mínimo absoluto de 1 segundo.
- Configuración:
  - Unificados los tiempos de reproducción de aldeanos y piglins en `breederTicks`.
  - Unificados los tiempos de crecimiento de ambas incubadoras en `incubatorTicks`.
  - `villagerInfiniteTrades` movido a `[timers]` y activado por defecto.
  - Eliminadas las opciones de ajuste detallado solicitadas; sus valores quedan internos y estables.
- Criadero de Piglins:
  - Los bebés generados eliminan datos de objetos en manos, armadura y probabilidades de drop.


## 2026-07-13 — Descuentos, UI, arquitectura y capturadores

- Sustituido el descuento permanente global por un descuento temporal de una unidad, aplicable una sola vez a cada oferta distinta durante 12 000 ticks.
- Los descuentos de curación continúan acumulándose y siempre respetan el precio mínimo de un objeto.
- Sincronización periódica de precios mientras el Trocador permanece abierto para evitar desapariciones visuales.
- El Autotrocador muestra el precio original tachado y el precio reducido a su lado.
- El botón de reinicio solo aparece con aldeanos sin experiencia profesional y se ha movido a la izquierda de la primera fila de entrada.
- Los capturadores eliminan UUID y otros datos volátiles al guardar, y comprueban colisión antes de liberar.
- Añadida `ARCHITECTURE_AUDIT.md`.

## 2026-07-13 - Inventario alineado y equipo lateral

- Centralizadas las coordenadas del inventario del jugador en `MachineScreenLayout`.
- Redibujados los 36 marcos del inventario desde las mismas coordenadas usadas por `addStandardInventorySlots`, eliminando el desfase entre textura y slots interactivos.
- Añadida una cabecera visual para el texto `Inventario` encima de la sección del jugador.
- Añadido un panel lateral izquierdo con acceso directo a casco, pechera, pantalones, botas y mano secundaria.
- Los slots de armadura respetan las reglas vanilla, incluidos el tipo de pieza, el límite de una unidad y la maldición de ligadura.
- Aplicado a Autotrocador, Criaderos, Convertidor, Cultivo, Incubadoras y Granja de Hierro.
- El Trocador de Aldeanos normal continúa usando la interfaz vanilla de comercio y no genera una pantalla propia del mod.

## 2026-07-13 — Rediseño homogéneo de las interfaces de máquinas

- Sustituido el recorte de la textura del Criadero de Aldeanos que se reutilizaba en todos los menús y provocaba marcos amarillos, márgenes blancos y desalineaciones.
- El ancho real de las interfaces pasa a incluir el panel de equipo; ya no se dibujan slots con coordenadas negativas fuera del área del menú.
- Añadidas paletas visuales específicas para Autotrocador, Criaderos, Convertidor, Cultivo, Incubadoras y Granja de Hierro.
- Los marcos de los slots del inventario, la barra rápida, la armadura y la mano secundaria usan ahora la misma temática que la máquina abierta.
- Unificadas las coordenadas visuales y lógicas mediante `MachineMenuLayout` y `MachineScreenLayout`.
- `MachineMenuLayout` no depende de clases de cliente, evitando que los menús comunes carguen código gráfico en servidor dedicado.
- Restaurado el rótulo vanilla `Inventario` en una cabecera propia, sin superposición con slots de la máquina.
- Reordenados los slots inferiores de los criaderos para que no invadan la cabecera del inventario.
- Los seis tipos de pantalla personalizados usan ahora el mismo sistema de marco, inventario, panel de equipo y progreso, manteniendo únicamente su distribución funcional y su paleta propia.

## Refinado visual de menús (2026-07-13)

- Cabecera temática común con el nombre de cada bloque entidad centrado y enmarcado.
- Menú reducido a 204x212 para evitar recortes con escalas de interfaz altas.
- Eliminado el gran carril vacío de la izquierda; la armadura y la mano secundaria usan ahora un panel compacto junto al inventario.
- Marcos de slots dibujados un píxel fuera del área 16x16 del objeto, evitando que los ítems tapen el borde.
- Texto vanilla «Inventario» restaurado dentro de una cabecera propia.
- Barras de progreso desplazadas un píxel arriba y un píxel a la derecha, recortando un píxel de anchura por el extremo.
- Criaderos reorganizados de forma simétrica: comida, progenitores, cama, progreso, bebé, capturador vacío y salida capturada.
- Añadidas siluetas de cabeza de aldeano y piglin para los slots de progenitores; los colmillos del piglin son blancos.
- El selector de variante del aldeano queda pegado al slot de comida y su lista se limita a cuatro filas visibles.
- Verificada la automatización de criaderos: comida por arriba, capturadores por los laterales y bebé capturado por abajo.
- Autotrocador: botón de reinicio a la derecha de la salida, eliminada la «V» y añadidos iconos de tolva para las dos entradas laterales.
- Granja de hierro: información de aldeanos y eficiencia en blanco; eliminado el texto «Máximo» cuando se completa la máquina.

## 2026-07-13 — Botones, barras, Autotrocador y XP acumulada

- Sustituido el icono de reinicio por un botón pixel-art de 16×16 con marco marrón y flecha circular verde, incluida su variante resaltada.
- Corregido el relleno de todas las barras de progreso: empieza dos píxeles dentro del marco, ocupa la fila superior interior y termina un píxel antes del borde derecho.
- Granja de Hierro:
  - La casilla «Amapolas» se desplaza exactamente un slot hacia la izquierda.
  - Los textos de aldeanos y eficiencia bajan diez píxeles.
- Autotrocador de Aldeanos:
  - Eliminados los iconos de tolva que se solapaban con los slots.
  - Añadidas flechas compactas junto a cada fila de entrada.
  - La segunda fila de entradas sube dos píxeles.
  - El botón de XP baja dos píxeles.
- Trocador de Aldeanos:
  - La experiencia de cada intercambio se acumula en el bloque en lugar de crear orbes inmediatamente.
  - Añadido bajo la parte derecha del menú vanilla el mismo botón de extracción de XP del Autotrocador.
  - La experiencia almacenada se guarda en NBT y se sincroniza mientras la interfaz permanece abierta.
- El marco temático del inventario y la barra rápida llega ahora hasta el borde inferior, igual que el panel de armadura.

## 2026-07-13 — Reinicio de intercambios y barras

- Rediseñados los sprites de reinicio con una flecha circular pixel-art claramente definida.
- La tecla `C` reinicia los intercambios del Autotrocador cuando la acción está disponible.
- Reiniciar ya no cierra el desplegable de ofertas del Autotrocador.
- Los slots de salida del Autotrocador se han desplazado 2 píxeles hacia arriba.
- En el Trocador vanilla, el botón de reinicio se coloca a la derecha de la primera oferta.
- El botón de XP permanece bajo el lado derecho del menú y la XP continúa acumulándose en el bloque.
- Las barras de progreso rellenan también la fila interior inferior; se aumentó la altura, no se desplazó `Y`.

## 2026-07-13 — Nueva GUI compartida del Trocador y Autotrocador

- Sustituida la interfaz vanilla del Trocador de Aldeanos por un menú propio registrado por el mod.
- Atlas compartido de `512×256` con área visible centrada de `348×194`, respetando el límite de 30 píxeles adicionales en altura.
- El Trocador y el Autotrocador comparten fondo, profesión, progreso, previsualización, depósito de XP, reset, inventario y equipamiento.
- Añadidas variantes completas `DEFAULT`, `PLAINS`, `DESERT`, `SAVANNA`, `TAIGA`, `SNOW`, `SWAMP` y `JUNGLE`.
- Los temas se resuelven desde `VillagerData` y contienen escenas pixel-art propias colocadas en una zona decorativa segura.
- Armadura y mano secundaria organizadas en una sola columna a la derecha del inventario, con sprites propios del mod.
- Trocador manual: siete ofertas visibles, clipping, rueda, arrastre, paginación y navegación completa por teclado.
- Autotrocador: desplegable de cuatro ofertas, buffers 4+4+4, bloqueo de slots durante el desplegable y segunda entrada deshabilitada cuando no se necesita.
- Los cambios de oferta y reinicios simulan primero la devolución de objetos y se cancelan si no existe espacio suficiente.
- Selección y reinicio del Autotrocador validan la revisión conocida de las ofertas en el servidor.
- La XP se almacena en los bloques: clic normal extrae todo y Mayús + clic completa únicamente el siguiente nivel.
- El sonido de reinicio solo se reproduce después de que el servidor confirme que las ofertas cambiaron realmente.
- Añadido el generador reproducible `tools/generate_villager_trade_gui.py`.

## 2026-07-25 - XP, recursos y límites hexagonales

- Eliminada la franja verde superior del fondo compartido del Trader y Autotrader.
- Sustituida la esmeralda de XP por el orbe vanilla, centrado en un panel integrado con relieve.
- La experiencia profesional usa los sprites vanilla de la barra de aldeano.
- Aumentado el contraste de todas las flechas de intercambio.
- Reorganizadas las texturas por fondos, filas, flechas, selector, slots, capturadores y reinicio.
- Los adaptadores de cada feature dependen de puertos de entrada; `FeatureComposition` es el único punto de construcción de servicios.
- Trader, Autotrader, trueque de piglins y reglas de aldeanos quedan reunidos en `feature/trader`.
- `captures` conserva una API publica limitada y `TimedProcess` pasa al kernel puro `shared/machines`.
- La configuracion global se divide en puertos de salida propiedad de cada feature.
- `checkArchitecture` impide dependencias de adaptadores hacia servicios concretos, configuración global o features no permitidas.
- Verificados Trader y Autotrader dentro del mundo `Test`.
