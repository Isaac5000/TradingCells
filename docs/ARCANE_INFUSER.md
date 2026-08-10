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

### Nitwit

```text
Patata venenosa | Tinte verde | Patata venenosa
Hongo marrón    | Capturador con aldeano sin empleo | Hongo rojo
Patata venenosa | Reloj       | Patata venenosa
```

Coste: `5.000` XP. El resultado conserva el capturador, su variante, durabilidad y datos del aldeano, cambiando únicamente su profesión a Nitwit.

### Granja de Esqueletos

```text
Cabeza de esqueleto | Hielo azul       | Cabeza de esqueleto Wither
Arco                | Estrella Nether  | Patata venenosa
Bloque de huesos    | Arena de almas   | Arenisca
```

Coste: `25.000` XP. Los ingredientes representan al esqueleto normal, Wither, de hielo, de pantano y del desierto. Produce el bloque de Granja de Esqueletos.

### Toque del Guerrero

```text
Cabeza de esqueleto | Fragmento de eco | Cabeza de esqueleto Wither
Tótem                | Libro            | Estrella del Nether
Bloque de huesos     | Bloque netherita | Espada de diamante
```

Coste: `45.000` XP. Produce un libro que evita el desgaste de espadas dentro de la Granja de Esqueletos.

Las recetas usan el tipo `trading_cells:arcane_infusion` y pueden ampliarse mediante datapacks.

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

Un Warden eliminado por un jugador añade un fragmento de eco a su botín normal. Saqueo puede añadir aleatoriamente entre cero y su nivel al fragmento. Para el catalizador de sculk garantiza entre uno y su nivel de unidades adicionales, por lo que cualquier nivel de Saqueo siempre mejora la unidad base.
