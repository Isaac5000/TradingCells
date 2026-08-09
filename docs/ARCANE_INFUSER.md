# Infusor Arcano

El Infusor Arcano convierte libros normales en libros con Toque del Granjero o Toque del Minero. Es una máquina portátil e instantánea que conserva inventario, experiencia y modo de fabricación al romperse y recolocarse.

## Distribución

```text
                 Ingrediente de receta
                          |
Tótem de la inmortalidad - Libro - Estrella del Nether  ->  Resultado
                          |
                 Bloque de netherita
```

Los cinco huecos guardan cualquier objeto para permitir futuras recetas. Las recetas actuales exigen un `minecraft:book` sin componentes adicionales en el centro. El depósito admite hasta `2.147.483.647` puntos de experiencia. Cada punto equivale a una unidad de `trading_cells:liquid_experience`; el coste depende de la receta.

## Infusiones

### Toque del Granjero

- 1 libro normal.
- 16 alientos de dragón.
- 1 tótem de la inmortalidad.
- 1 estrella del Nether.
- 1 bloque de netherita.
- 15.000 puntos de experiencia.

### Toque del Minero

- 1 libro normal.
- 1 fragmento de eco.
- 1 tótem de la inmortalidad.
- 1 estrella del Nether.
- 1 bloque de netherita.
- 30.000 puntos de experiencia.

Las recetas usan el tipo de datos `trading_cells:arcane_infusion`; se pueden añadir nuevas infusiones mediante datapacks sin cambiar el código de la máquina.

## Funcionamiento

- En modo manual, la salida es una previsualización: los ingredientes y el XP solo se consumen cuando el jugador retira el resultado, igual que en una mesa de trabajo.
- En modo automático, completa como máximo una infusión por tick y guarda el resultado en el hueco físico de salida.
- Una receta válida sin experiencia suficiente muestra el resultado atenuado y bloquea su extracción.
- El marcador inferior muestra `XP disponible/XP necesario` y limita visualmente el primer valor al coste de la receta.
- El botón superior alterna entre ambos modos; el modo manual es el valor inicial.
- Ingredientes y experiencia se consumen en una única operación atómica.
- Si faltan recursos o la salida está ocupada, no se consume nada.
- Al completarse, reproduce un sonido y una ráfaga breve de partículas.
- No existe selector de receta, botón de inicio ni barra de progreso.
- La flecha del menú abre directamente la categoría de Infusión Arcana cuando REI está instalado.

## Automatización

- Los ingredientes pueden insertarse desde cualquier cara y permanecen en sus huecos hasta usarse o retirarse.
- Las tuberías y tolvas solo extraen resultados ya fabricados en modo automático; nunca pueden extraer la previsualización manual.
- La experiencia líquida puede insertarse y extraerse mediante la capacidad de fluidos de NeoForge.
- El menú también permite almacenar o retirar niveles del jugador; una cantidad vacía transfiere todo lo posible.
- La máquina permanece inactiva mientras espera recursos y se despierta al recibir cambios.

## Receta del bloque

```text
Obsidiana llorosa | Mesa de encantamientos          | Obsidiana llorosa
Obsidiana llorosa | Almacén de Experiencia vacío   | Obsidiana llorosa
Obsidiana llorosa | Obsidiana llorosa               | Obsidiana llorosa
```

Un Almacén de Experiencia con datos guardados no es un ingrediente válido, lo que evita perder experiencia accidentalmente.

En el mundo, los cuatro ingredientes exteriores se muestran sobre pedestales de obsidiana llorosa y el ingrediente central aparece sobre una mesa de encantamientos sin libro.

## Fragmentos de eco

Los fragmentos siguen apareciendo en cofres de ciudades antiguas. Además, un Warden eliminado por un jugador añade un fragmento garantizado a su botín normal. Saqueo añade una cantidad aleatoria entre cero y el nivel del encantamiento; las muertes sin jugador no reciben este botín adicional.
