# Granja de Esqueletos

La Granja de Esqueletos es una máquina portátil con un menú de `348 x 210`, la misma geometría e inventario del Trader. Conserva trabajador, espada, dieciocho salidas, objetivo, filtros, progreso, botín pendiente y experiencia al romperse y recolocarse.

La pantalla conserva una copia propia del fondo, medidas y paleta del Trader. Cambios posteriores en una interfaz no alteran la otra. El TAB de Granjas agrupa las granjas de criaturas.

## Objetivos y filtros

- Esqueleto normal: armas, huesos, flechas y cabeza con Decapitación.
- Esqueleto Wither: armas, huesos, cabezas y carbón.
- Esqueleto de hielo: armas, huesos, flechas de lentitud y cabeza con Decapitación.
- Esqueleto de pantano: armas, huesos, flechas de veneno y cabeza con Decapitación.
- Esqueleto del desierto: armas, huesos, flechas de debilidad y cabeza con Decapitación.
- Caballo esqueleto: huesos. Su huevo se fabrica por separado y no participa en la receta del bloque.

El selector de objetivo muestra hasta ocho variantes y la lista de recompensas es desplazable. Cada recompensa compatible puede activarse o desactivarse sin alterar las tiradas aleatorias de las demás. Todos los textos traducidos se mantienen en una línea y reducen su escala cuando el idioma necesita más espacio.

El botón `?` del menú normal, situado bajo el trabajador y a la izquierda de las salidas, abre hasta seis resultados por página. Sus cantidades y probabilidades se recalculan con los filtros activos, Botín, Filo Arrasador, trabajador, espada y estado de pausa actuales.

REI representa cada objetivo mediante su huevo generador. El Infusor Arcano puede transformar cualquier huevo normal admitido por `#trading_cells:arcane_infusion_eggs` en una de las seis variantes por entre `55` y `160` XP.

## Herramientas

- El nivel de la espada reduce el tiempo siguiendo la misma curva que Cultivos y Canteras: madera tarda `120 s`, Netherite `20 s` y los niveles superiores se aproximan a un mínimo de `1 s`.
- Golpeo reduce el tiempo hasta nivel V; con Netherite y Golpeo V el ciclo dura `5 s`.
- Filo también reduce el tiempo mediante su daño adicional, pero siempre menos que Golpeo al mismo nivel.
- Encantamientos de otros mods que aporten daño mediante el efecto estándar de Minecraft reducen el ciclo con el mismo límite. Efectos personalizados sin contrato de daño o botín no se pueden inferir automáticamente.
- Botín aumenta las cantidades y probabilidades de botín.
- Filo Arrasador añade una baja simulada por nivel.
- Irrompibilidad se aplica mediante el desgaste vanilla de la espada.
- Toque del Guerrero evita por completo ese desgaste en todas las granjas de criaturas del mod.
- Decapitación I-VI habilita cabezas para las variantes que no las sueltan normalmente. Su probabilidad va de `3,5 %` a `8,5 %` y no depende de Botín.

En esqueletos Wither, Botín y Decapitación aportan cada uno su nivel a una única curva `2,5 % + 1 %` por nivel, con un máximo de una cabeza por entidad. Fuera de la granja funciona en espadas y hachas para dragones, zombis, esqueletos, esqueletos Wither, jugadores, creepers y piglins. Los aldeanos nunca generan cabezas de jugador, aunque tengan un nombre personalizado.

Para mods reconoce automáticamente los identificadores convencionales `<entidad>_head`, `<entidad>_skull`, `head_<entidad>` y `skull_<entidad>`. Primero comprueba el espacio de nombres de la entidad y después cualquier cabeza de otro mod incluida en `#minecraft:skulls`; la caché se reconstruye al recargar datapacks. Minecraft no ofrece un registro universal entidad-cabeza, por lo que identificadores no convencionales no pueden inferirse con seguridad y no generan objetos inventados.

La granja acumula cinco puntos de XP por baja simulada. El cuadro de XP y el botón de retirada usan el estilo del Autotrader. Un control persistente permite pausar la granja sin perder progreso ni gastar durabilidad.

## Automatización

Los laterales y la cara superior aceptan un aldeano adulto capturado y una espada compatible. La cara inferior solo extrae las dieciocho salidas. El trabajador y la espada no se consumen; la espada puede perder un punto de durabilidad por ciclo.

## REI

REI ofrece una vista independiente para cada objetivo, muestra su espada compatible y alterna todas sus recompensas en un único slot. El tooltip de cada resultado conserva su cantidad y probabilidad base por baja simulada, sin añadir controles que no representen el estado real de una máquina. La categoría se abre directamente desde la barra de progreso del menú.

## Compatibilidad con esqueletos externos

Las entidades monstruosas añadidas a `#minecraft:skeletons` se incorporan automáticamente al selector. El caballo esqueleto forma parte expresamente del catálogo aunque no pertenezca a la categoría monstruosa. Trading Cells inspecciona sus tablas de botín cargadas y crea un filtro independiente para cada objeto que no pertenezca ya a Armas, Huesos, Flechas, Cabezas o Carbón. Un fallo aislado descarta únicamente esa tabla o entidad; si el catálogo completo no puede reconstruirse, permanecen las seis variantes fijas.

El descubrimiento enumera entradas de objetos y tags de las tablas de datos. No puede anticipar botín añadido exclusivamente durante la ejecución por código o por modificadores globales opacos, ni calcular una probabilidad fiable para condiciones arbitrarias; esos objetos siguen pudiendo producirse mediante la tabla real, pero no se inventan porcentajes.

En el mundo, la base usa musgo pálido. El spawner se muestra entero y proporcionado al aldeano; el objetivo activo permanece inmóvil sobre él. La entidad y el aldeano se reutilizan entre fotogramas, y solo se recalcula su estado cuando la máquina visible lo necesita.
