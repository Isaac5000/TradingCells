# Infusor Arcano

El Infusor Arcano es una máquina portátil y manual con una matriz de nueve ingredientes. Conserva su inventario y hasta `2.147.483.647` puntos de experiencia al romperse y recolocarse.

## Distribución

```text
Entrada | Entrada | Entrada
Entrada | Centro  | Entrada  -> Resultado
Entrada | Entrada | Entrada
```

Los nueve huecos guardan cualquier objeto. Una receta valida se previsualiza en
la salida, pero los ingredientes y el XP solo se consumen al retirar el
resultado, como en una mesa de trabajo. No existe modo automatico ni
transferencia de objetos mediante tolvas o tuberias.

El boton de libro lila abre el recetario propio a la izquierda y desplaza el
menu a la derecha cuando hay espacio, sin solaparlos. El boton queda sobre la
salida de la receta. Incluye buscador, filtro de todas/solo
fabricables y las categorias Generadores, Equipo, Produccion y Varios. Todas las
infusiones estan desbloqueadas desde el principio y no generan avisos de receta.
Un clic coloca un lote; Shift coloca el maximo de lotes completos. El servidor
comprueba cantidades y componentes exactos antes de modificar el inventario.

Una receta puede exigir huecos vacios con `{"empty": true}` en cualquiera de
sus nueve posiciones. El campo opcional `category` acepta `generators`,
`equipment`, `production` o `misc`; si se omite usa `misc`.

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
Huevo de Stray      | Almacén de XP     | Huevo de Bogged
Spawner              | Bloque de musgo pálido | Bloque de hierro
```

Coste: `50.000` XP. Usa directamente los huevos generadores de las cinco variantes compatibles y produce el bloque de Granja de Esqueletos.

### Huevos generadores de esqueletos

Seis recetas transforman un huevo normal admitido por `#trading_cells:arcane_infusion_eggs` en el huevo generador objetivo. La etiqueta incluye `#minecraft:eggs`, el huevo de tortuga y el huevo de sniffer, y puede ampliarse mediante datapacks o mods. Los ingredientes exteriores combinan huesos, armas, flechas y materiales representativos de cada variante. El caballo esqueleto está disponible en el selector y tiene su propia infusión, pero no participa en la receta de la granja.

- Esqueleto normal: `60` XP.
- Esqueleto de hielo: `100` XP.
- Esqueleto de pantano: `140` XP.
- Esqueleto del desierto: `140` XP.
- Esqueleto Wither: `300` XP.
- Caballo esqueleto: `180` XP.

### Granja de Zombis

```text
Huevo de zombi      | Huevo de piglin z.  | Huevo de aldeano zombi
Huevo de momificado | Almacén de XP       | Huevo de ahogado
Spawner              | Bloque de musgo pálido | Bloque de hierro
```

Coste: `50.000` XP. Produce la Granja de Zombis. El zoglin está disponible en el selector, pero no participa en esta receta.

### Huevos generadores de zombis

Nueve recetas convierten `#trading_cells:arcane_infusion_eggs` en huevos de
zombi, aldeano zombi, momificado, ahogado, piglin zombificado, zoglin, caballo
zombi, Camel Husk o Zombie Nautilus. Sus costes son `60`, `120`, `100`, `200`,
`160`, `250`, `180`, `200` y `200` XP respectivamente.

### Granja de Saqueadores

```text
Huevo de saqueador | Frasco ominoso      | Huevo de devastador
Huevo de bruja     | Almacén de XP       | Huevo de invocador
Spawner             | Tablones de roble oscuro | Bloque de hierro
```

Coste: `100.000` XP. Cualquier nivel de frasco ominoso es válido. La máquina
incluye saqueador, invocador, vindicador, ilusionista, devastador, bruja y vex;
el saqueador normal puede producir su estandarte ominoso. También admite
objetivos registrados por tags o datapacks.

### Huevos generadores de saqueadores

Seis recetas convierten `#trading_cells:arcane_infusion_eggs` en huevos de
saqueador, invocador, devastador, bruja, vindicador o vex. Sus costes son `100`,
`350`, `450`, `250`, `200` y `300` XP respectivamente. Illusioner puede entrar
por el tag vanilla de saqueadores, pero Minecraft no proporciona un huevo para
crear.

### Granja de Creepers

```text
Pólvora                    | Huevo de creeper     | Pólvora
Fragmento de la Tormenta  | Almacén de XP        | Fragmento de la Tormenta
Spawner                    | Bloque de musgo pálido | Bloque de hierro
```

Coste: `25.000` XP. La máquina usa musgo pálido en su base, permite alternar entre
Creeper normal y cargado y admite objetivos de mods mediante tags o datapacks.

### Huevo generador de Creeper

Una infusion de `80` XP convierte un huevo normal admitido en un huevo de
Creeper usando pólvora, TNT, musgo y un mechero.

### Nuevas familias de granjas

Todas usan Almacen de XP en el centro y la fila inferior `Spawner | bloque base
| Bloque de hierro`. Los cinco huecos restantes combinan Spawn Eggs y materiales
tematicos. Sus objetivos se amplian mediante tags y descriptores de datapack.

| Granja | Objetivos integrados | Base | Coste |
| --- | --- | --- | ---: |
| Artropodos | Spider, Cave Spider, Silverfish y Endermite | Pale Moss Block | 35.000 XP |
| Slimes | Slime, Magma Cube y Sulfur Cube | Slime Block | 45.000 XP |
| Guardianes | Guardian y Elder Guardian | Prismarine Bricks | 75.000 XP |
| Piglins | Piglin y Piglin Brute | Polished Blackstone | 70.000 XP |
| Blazes | Blaze | Nether Bricks | 55.000 XP |
| Ghasts | Ghast y Happy Ghast | Soul Soil | 65.000 XP |
| Endermen | Enderman | End Stone | 60.000 XP |
| Shulkers | Shulker | Purpur Block | 90.000 XP |
| Breezes | Breeze | Polished Tuff | 80.000 XP |
| Phantoms | Phantom | Pale Moss Block | 40.000 XP |
| Animales | Cow, Mooshroom, Sheep, Pig, Chicken, Rabbit y Goat | Hay Block | 20.000 XP |
| Peces | Cod, Salmon, Tropical Fish y Pufferfish | Sand | 20.000 XP |
| Acuaticas | Squid, Glow Squid, Dolphin y Nautilus | Dark Prismarine | 30.000 XP |
| Monturas | Horse, Donkey, Mule, Camel, Llama y Trader Llama | Oak Planks | 30.000 XP |
| Anfibios | Axolotl, Frog, Tadpole y Turtle | Mud | 25.000 XP |
| Abejas | Bee | Honeycomb Block | 25.000 XP |
| Creakings | Creaking | Pale Moss Block | 85.000 XP |

### Huevos generadores de las nuevas familias

| Familia | Costes de sus huevos |
| --- | --- |
| Artropodos | Spider `80`, Cave Spider `120`, Silverfish `100`, Endermite `140` XP |
| Slimes | Slime `80`, Magma Cube `180`, Sulfur Cube `200` XP |
| Guardianes | Guardian `220`, Elder Guardian `550` XP |
| Piglins | Piglin `180`, Piglin Brute `320` XP |
| Blaze | `250` XP |
| Ghasts | Ghast `280`, Happy Ghast `280` XP |
| Enderman | `250` XP |
| Shulker | `350` XP |
| Breeze | `300` XP |
| Phantom | `180` XP |
| Animales | Chicken `80`, Rabbit `100`, Pig `120`, Sheep `140`, Cow `160`, Goat `180`, Mooshroom `220` XP |
| Peces | Cod `80`, Salmon `100`, Tropical Fish `120`, Pufferfish `140` XP |
| Acuaticas | Squid `80`, Glow Squid `120`, Dolphin `180`, Nautilus `220` XP |
| Monturas | Horse `160`, Donkey `140`, Mule `180`, Camel `180`, Llama `160`, Trader Llama `220` XP |
| Anfibios | Axolotl `180`, Frog `120`, Tadpole `80`, Turtle `180` XP |
| Abeja | `140` XP |
| Creaking | `500` XP |

El limite de cualquier infusión de Spawn Egg es `550` XP, equivalente a veinte
niveles completos. Los costes son multiplos de diez.

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
- El marcador inferior muestra `XP almacenada/XP necesaria`, incluida la receta
  fantasma seleccionada en el recetario, sin limitar visualmente el primer valor.
- Ingredientes y experiencia se consumen en una única operación atómica.
- Si falta cualquier recurso, no se consume nada.
- Los ingredientes con recipiente de fabricación, como los cubos de agua o lava,
  dejan el recipiente vacío en su mismo hueco.
- Al completarse, reproduce un sonido y una ráfaga breve de partículas.
- La flecha del menú abre la categoría de Infusión Arcana cuando REI está instalado.
- El botón de transferencia de REI mueve los nueve ingredientes a su posición cuando el menú del Infusor está abierto. No comprueba ni transfiere experiencia.
- El recetario vanilla adaptado funciona sin REI y conserva exactamente los
  componentes de libros encantados y capturadores.
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
