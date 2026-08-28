# Infusor Arcano

El Infusor Arcano es una máquina portátil y manual con una matriz de nueve ingredientes. Conserva su inventario y hasta `2.147.483.647` puntos de experiencia al romperse y recolocarse.

## Distribución

```text
Entrada | Entrada | Entrada
Entrada | Centro  | Entrada  -> Resultado
Entrada | Entrada | Entrada
```

Los nueve huecos guardan cualquier objeto. Una receta válida se previsualiza en la salida, pero los ingredientes y el XP solo se consumen al retirar el resultado, como en una mesa de trabajo. No existe modo automático ni transferencia de objetos mediante tolvas o tuberías.

## Infusiones

### Toque del Granjero

```text
Patata | Flor de chorus | Remolacha
Tótem  | Libro          | Estrella del Nether
Trigo  | Bloque de netherita | Zanahoria
```

Coste: `15.000` XP.

### Toque del Minero

```text
Piedra del End     | Fragmento de eco | Catalizador de sculk
Tótem               | Libro            | Estrella del Nether
Catalizador de sculk | Bloque de netherita | Piedra del End
```

Coste: `30.000` XP.

### Toque de Seda II

```text
Fragmento de eco | Fragmento de amatista | Fragmento de eco
Huevo de tortuga | Libro con Toque de Seda I | Huevo de tortuga
Fragmento de eco | Estrella del Nether | Fragmento de eco
```

Coste: `75.000` XP. Produce `minecraft:silk_touch` a nivel 2, no un
encantamiento alternativo. Se aplica en yunque a las mismas herramientas que el
Toque de Seda vanilla y permite recoger los bloques enumerados en
[`SILK_TOUCH_II.md`](SILK_TOUCH_II.md).

### Nitwit

```text
Pluma             | Tinte verde | Pluma
Membrana de fantasma | Capturador con aldeano sin empleo | Membrana de fantasma
Pluma             | Reloj       | Pluma
```

Coste: `5.000` XP. El resultado conserva el capturador, su variante, durabilidad y datos del aldeano, cambiando únicamente su profesión a Nitwit.

### Granja de Esqueletos

```text
Huevo de esqueleto  | Huevo de Parched  | Huevo de esqueleto Wither
Huevo de Stray      | Spawner           | Huevo de Bogged
Lingote de hierro   | Bloque de musgo pálido | Lingote de hierro
```

Coste: `50.000` XP. Usa directamente los huevos generadores de las cinco variantes compatibles y produce el bloque de Granja de Esqueletos.

### Huevos generadores de esqueletos

Seis recetas transforman un huevo normal admitido por `#trading_cells:arcane_infusion_eggs` en el huevo generador objetivo. La etiqueta incluye `#minecraft:eggs`, el huevo de tortuga y el huevo de sniffer, y puede ampliarse mediante datapacks o mods. Los ingredientes exteriores combinan huesos, armas, flechas y materiales representativos de cada variante. El caballo esqueleto está disponible en el selector y tiene su propia infusión, pero no participa en la receta de la granja.

- Esqueleto normal: `55` XP, equivalente al nivel 5.
- Esqueleto de hielo: `91` XP, equivalente al nivel 7.
- Esqueleto de pantano: `112` XP, equivalente al nivel 8.
- Esqueleto del desierto: `112` XP, equivalente al nivel 8.
- Esqueleto Wither: `160` XP, equivalente al nivel 10.
- Caballo esqueleto: `139` XP, equivalente al nivel 9.

### Granja de Zombis

```text
Huevo de zombi      | Bloque de musgo pálido | Huevo de aldeano zombi
Huevo de momificado | Spawner             | Huevo de ahogado
Lingote de hierro   | Huevo de piglin z.  | Lingote de hierro
```

Coste: `50.000` XP. Produce la Granja de Zombis. El zoglin está disponible en el selector, pero no participa en esta receta.

### Huevos generadores de zombis

Seis recetas convierten `#trading_cells:arcane_infusion_eggs` en huevos de zombi, aldeano zombi, momificado, ahogado, piglin zombificado o zoglin. Los costes son `55`, `91`, `76`, `160`, `112` y `139` XP respectivamente.

### Granja de Saqueadores

```text
Huevo de saqueador | Frasco ominoso      | Huevo de devastador
Huevo de bruja     | Spawner             | Huevo de invocador
Lingote de hierro  | Tablones de roble oscuro | Lingote de hierro
```

Coste: `100.000` XP. Cualquier nivel de frasco ominoso es válido. La máquina
incluye saqueador, invocador, devastador y bruja; el saqueador normal puede
producir su estandarte ominoso. También admite objetivos registrados por tags o
datapacks.

### Huevos generadores de saqueadores

Cuatro recetas convierten `#trading_cells:arcane_infusion_eggs` en huevos de
saqueador, invocador, devastador o bruja. Sus costes son `80`, `150`, `150` y
`100` XP respectivamente.

### Granja de Creepers

```text
Lingote de hierro         | Huevo de creeper | Lingote de hierro
Fragmento de la Tormenta  | Spawner          | Fragmento de la Tormenta
Lingote de hierro         | Bloque de musgo  | Lingote de hierro
```

Coste: `25.000` XP. La máquina permite alternar entre Creeper normal y cargado;
los objetivos de mods pueden añadirse mediante tags o datapacks.

### Huevo generador de Creeper

Una infusión de `75` XP convierte un huevo normal admitido en un huevo de
Creeper usando pólvora, TNT, musgo y un mechero.

### Toque del Guerrero

```text
Cabeza de esqueleto | Fragmento de eco | Cabeza de esqueleto Wither
Tótem                | Libro            | Estrella del Nether
Bloque de huesos     | Bloque netherita | Espada de diamante
```

Coste: `45.000` XP. Produce un libro que evita el desgaste de espadas dentro de las granjas de criaturas del mod.

### Decapitación

```text
Fragmento de la Tormenta | Cabeza de dragón           | Fragmento de la Tormenta
Aliento de dragón        | Libro                      | Aliento de dragón
Fragmento de la Tormenta | Cabeza de esqueleto Wither | Fragmento de la Tormenta
```

Coste: `25.000` XP. Produce un libro de Decapitación I aplicable a espadas y hachas. Cada esquina consume un Fragmento de la Tormenta. Los niveles I-VI se obtienen combinando libros de forma convencional o colocando en la mesa de herrería un libro o arma que ya tenga Decapitación junto a un único Fragmento de la Tormenta; el fragmento aumenta exactamente un nivel y no depende de Botín. REI muestra esta mejora como receta de herrería.

Las recetas usan el tipo `trading_cells:arcane_infusion` y pueden ampliarse mediante datapacks.

## Receta del spawner

```text
Barrotes de hierro | Obsidiana           | Barrotes de hierro
Obsidiana          | Estrella del Nether | Obsidiana
Barrotes de hierro | Aliento de dragón   | Barrotes de hierro
```

Esta receta hace renovable el ingrediente central de la Granja de Esqueletos sin abaratar su progresión.

## Funcionamiento

- Una receta válida sin experiencia suficiente muestra el resultado atenuado y bloquea su extracción.
- El marcador inferior muestra `XP disponible/XP necesario` y limita visualmente el primer valor al coste de la receta.
- Ingredientes y experiencia se consumen en una única operación atómica.
- Si falta cualquier recurso, no se consume nada.
- Al completarse, reproduce un sonido y una ráfaga breve de partículas.
- La flecha del menú abre la categoría de Infusión Arcana cuando REI está instalado.
- El botón de transferencia de REI mueve los nueve ingredientes a su posición cuando el menú del Infusor está abierto. No comprueba ni transfiere experiencia.
- La experiencia líquida sí admite entrada y salida mediante la capacidad de fluidos de NeoForge.
- El menú permite almacenar o retirar niveles del jugador; una cantidad vacía transfiere todo lo posible.

## Receta del bloque

```text
Obsidiana llorosa | Mesa de encantamientos        | Obsidiana llorosa
Obsidiana llorosa | Almacén de Experiencia vacío  | Obsidiana llorosa
Obsidiana llorosa | Obsidiana llorosa             | Obsidiana llorosa
```

Un Almacén de Experiencia con datos guardados no es válido. En el mundo, los ocho ingredientes exteriores aparecen sobre pedestales de obsidiana llorosa y el ingrediente central sobre una mesa de encantamientos sin libro propio.

## Botín del Warden

Un Warden eliminado por un jugador añade un fragmento de eco a su botín normal. Botín puede añadir aleatoriamente entre cero y su nivel al fragmento. Para el catalizador de sculk garantiza entre uno y su nivel de unidades adicionales, por lo que cualquier nivel de Botín siempre mejora la unidad base.
