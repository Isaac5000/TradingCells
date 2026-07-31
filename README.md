# Trading Cells

Trading Cells es un mod para NeoForge que añade capturadores de aldeanos y piglins, y una jaula de comercio orientable donde guardar una criatura capturada sin perder sus datos NBT.

## Contenido del mod

### Capturador de aldeanos (`trading_cells:villager_capturer`)

- **Shift + clic derecho** sobre un aldeano: guarda ese aldeano dentro del item.
- **Clic derecho** sobre un bloque normal con el capturador lleno: libera al aldeano guardado en el mundo.
- **Clic derecho** sobre una jaula vacía con el capturador lleno: mete el aldeano en la jaula.
- **Clic derecho** sobre una jaula con aldeano usando el capturador vacío: saca el aldeano de vuelta al capturador.
- El item brilla cuando contiene un aldeano.
- El render del item muestra al aldeano capturado en GUI, mano, tercera persona y decoraciones compatibles.
- Tiene 10 usos por defecto y pierde uno solo al liberar al aldeano; Irrompibilidad reduce el desgaste.

### Capturador de piglins (`trading_cells:piglin_capturer`)

- **Shift + clic derecho** sobre un piglin: guarda ese piglin dentro del item.
- **Clic derecho** sobre un bloque normal con el capturador lleno: libera al piglin guardado en el mundo.
- **Clic derecho** sobre una jaula vacía con el capturador lleno: mete el piglin en la jaula.
- **Clic derecho** sobre una jaula con piglin usando el capturador vacío: saca el piglin de vuelta al capturador.
- El item brilla cuando contiene un piglin.
- El render del item muestra al piglin capturado en GUI, mano, tercera persona y decoraciones compatibles.
- Tiene 10 usos por defecto y pierde uno solo al liberar al piglin; Irrompibilidad reduce el desgaste.

### Jaula de comercio (`trading_cells:villager_trading_cell`)

- Caja metálica con laterales y parte superior de cristal para ver la criatura y el POI interno.
- Tiene dirección horizontal al colocarla; la criatura renderizada mira hacia el frente de la jaula.
- Puede guardar aldeanos o piglins, pero solo los aldeanos pueden comerciar.
- **Clic derecho con mano vacía**, sin agacharte: abre la interfaz de comercio si dentro hay un aldeano adulto con comercios.
- **Shift + clic derecho con mano vacía**:
  - Si hay un POI guardado, lo retira primero y lo devuelve al inventario.
  - Si no hay POI, libera la criatura guardada al mundo.
- **Clic derecho con un bloque de trabajo de aldeano**: guarda ese bloque como POI interno.
- **Clic derecho con otro bloque de trabajo**: sustituye el POI si el oficio del aldeano todavía no es persistente.
- **Clic derecho con capturador vacío correcto**: saca la criatura de vuelta al capturador.
- Si rompes la jaula con contenido dentro, caen la criatura dentro de su capturador correspondiente y el POI guardado.

## POI y profesiones

La jaula acepta los bloques de trabajo vanilla de aldeano:

- Barril → pescador.
- Alto horno → armero.
- Soporte para pociones → clérigo.
- Mesa de cartografía → cartógrafo.
- Caldero → peletero.
- Compostador → granjero.
- Mesa de flechas → flechero.
- Piedra de afilar → armero de armas.
- Atril → bibliotecario.
- Telar → pastor.
- Mesa de herrería → herrero de herramientas.
- Ahumador → carnicero.
- Cortapiedras → albañil.

Si el aldeano aún no tiene profesión persistente, el POI interno define su profesión. Al quitar el POI, pierde esa profesión temporal. Si el aldeano ya tiene profesión persistente por haber comerciado o subido de nivel, la jaula no la sobrescribe.

## Comportamiento importante

- Los datos NBT del aldeano/piglin se conservan: profesión, comercios, nombre, experiencia, inventario interno y edad.
- Los datos temporales se limpian al capturar: posición, rotación, daño reciente, fuego y movimiento.
- Los aldeanos bebé pueden guardarse, pero no pueden comerciar hasta que sean adultos.
- La jaula guarda los usos de los comercios después de comerciar.
- Si intentas extraer con el capturador equivocado, el mod avisa y no borra la criatura.
- Para retirar el POI necesitas espacio en el inventario; si no hay espacio, el POI se queda dentro.

## Requisitos

- Trading Cells `1.0.0`
- Minecraft `26.2.0`
- NeoForge `26.2.0.40-beta`
- Java `25`

## Compatibilidad con REI

Roughly Enough Items es opcional. Con REI `26.2.820` o posterior, el visor
muestra las recetas normales del mod y los procesos de criaderos, incubadoras,
cultivo, conversión, granja de hierro y trueque de piglins, incluido el
trocador de Netherita.

## Configuración relevante

- `capturers.durability`: durabilidad común de ambos capturadores; por defecto, 10.
- `production.farmerDamageHoes`: hace que cada cosecha completada desgaste la azada; activado por defecto.
- `production.ironFarmMultiplierBonus`: suma el valor indicado a las bases `x1`, `x2` y `x3`.
- `Toque del Granjero` evita el desgaste de la azada dentro del Cultivo para Aldeanos. Por ahora su libro solo aparece en creativo.
- Los capturadores irrompibles existen como variantes creativas sin receta para una implementación posterior.

## Desarrollo

Compilar:

```bash
./gradlew build
```

Ejecutar cliente de desarrollo:

```bash
./gradlew runClient
```

El proyecto usa el mod id `trading_cells`.


## Cambios funcionales recientes

- La jaula de aldeanos ya no libera el aldeano con Shift + clic derecho; esa acción solo intenta retirar el POI.
- Los aldeanos bebé no reciben oficio del POI y no pueden reiniciar/intercambiar comercios.
- El aldeano bebé en primera persona conserva la misma escala base que el adulto; no se aumenta artificialmente.
- Se añade `piglin_bartering_cell`, una jaula separada para piglins. Guarda un piglin con el capturador de piglins y permite hacer trueque usando lingotes de oro, generando recompensas desde la loot table vanilla `minecraft:gameplay/piglin_bartering`.
