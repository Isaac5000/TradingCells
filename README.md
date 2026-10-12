# Trading Cells

Trading Cells es un mod para Minecraft 26.2 con NeoForge que automatiza el trabajo de aldeanos y piglins mediante máquinas portátiles. Conserva los datos de las criaturas, sus ofertas, inventarios y progresos cuando una máquina se rompe y vuelve a colocarse.

## Requisitos

- Minecraft `26.2.0`
- NeoForge `26.2.0.90` o posterior compatible con Minecraft 26.2
- Java `25`
- Roughly Enough Items `26.2.820` o posterior es opcional

La versión de desarrollo actual es `1.0.0` y todavía no se ha publicado.

## Granja de interacciones

La **Granja de interacciones** obtiene recursos sin matar ni capturar la criatura
del modelo. Usa un trabajador adulto, un Creature Model, un objeto y una
herramienta en canales independientes. Admite las mejoras de velocidad actuales,
tolvas y tuberías; no genera ni consume XP durante sus ciclos. Tiene 27 salidas
en tres filas de nueve y pausa manual, sin control de redstone ni comparador.

Se fabrica en el Infusor por **50.000 XP**, con cuatro bloques de lapislázuli,
cuatro de hormigón negro y un dispensador central. Receta visible en REI.
Contratos, compatibilidad y pruebas: [Granja de interacciones](docs/ENTITY_INTERACTION_FARM.md).

## Máquinas de aldeanos

- **Trocador de Aldeanos**: conserva un aldeano adulto, su POI, ofertas, descuentos y experiencia de comercio. Admite intercambio masivo con Mayús.
- **Autotrocador de Aldeanos**: ejecuta automáticamente la oferta elegida usando entradas y salidas automatizables.
- **Criadero de Aldeanos**: consume alimentos configurados y capturadores vacíos para producir aldeanos bebé.
- **Incubador de Aldeanos**: convierte un aldeano bebé capturado en adulto.
- **Cultivo de Aldeanos**: procesa cultivos, árboles, flores, plantas acuáticas, bloques de coral vivo, musgos y chorus del End con 27 salidas; el nivel de la azada y una mejora de velocidad independiente determinan el ciclo. Fortuna conserva su efecto sobre la cosecha.
- **Convertidor de Aldeanos**: zombifica y cura sin perder ofertas, y conserva los descuentos de curación.
- **Granja de Hierro**: produce hierro con multiplicadores base `x1`, `x2` y `x3` según los aldeanos instalados.
- **Granja de Esqueletos**: permite elegir entre esqueletos normales, Wither, de hielo, de pantano y del desierto, filtrar sus recompensas y mejorar la caza con una espada.
- **Granja de Zombis**: permite cazar zombis, aldeanos zombis, momificados, ahogados, piglins zombificados y zoglins con filtros de botín independientes.
- **Granja de Saqueadores**: reúne saqueadores, invocadores, vindicadores, ilusionistas, devastadores, brujas y vexes, con botín filtrable y objetivos externos ampliables.
- **Granja de Creepers**: procesa Creepers normales y cargados, incluida la obtención renovable de Fragmentos de la Tormenta.
- **Granjas configurables de criaturas**: añade Artrópodos, Slimes, Guardianes,
  Piglins, Blazes, Ghasts, Endermen, Shulkers, Breezes, Phantoms, Animales,
  Peces, Acuáticas, Monturas, Anfibios, Abejas y Creakings. Cada bloque acepta
  por tag criaturas equivalentes de otros mods.
- **Cantera de Aldeanos**: extrae materiales del Overworld con 27 salidas y una mejora de velocidad independiente. El pico conserva su nivel, Fortuna, Toque de Seda y desgaste.

## Máquinas de piglins

- **Trocador de Piglins**: automatiza la tabla de trueque vanilla.
- **Trocador de Piglins de Netherite**: añade 27 salidas, cinco niveles de mejora y filtros en el nivel de Netherite.
- **Criadero de Piglins** e **Incubador de Piglins**: reproducen y hacen crecer piglins capturados.
- **Cultivo de Piglins**: cultiva vegetación del Nether con 27 salidas y mejora de velocidad independiente; Fortuna aumenta cantidades y las probabilidades de hongos, bloques de verrugas y luz de hongo.
- **Cantera de Piglins**: extrae materiales del Nether con 27 salidas, mejora de velocidad independiente y minado profundo con las mejoras compatibles.

Las máquinas de comercio y trabajo rechazan aldeanos o piglins bebé cuando su función requiere una criatura adulta.

Las mejoras de velocidad actuales, de cobre a netherita, reducen el tiempo de
cultivos y canteras a la mitad por nivel, con un mínimo de 20 ticks. La mejora
de materiales de las canteras conserva su ranura independiente. La granja de
simulación de entidades también tiene 27 salidas y genera como máximo 10 XP
por criatura, antes de sumar los lotes; la XP ya almacenada se conserva.

## Capturadores

Los capturadores guardan la entidad completa y se apilan de uno en uno. Tienen diez liberaciones por defecto, pierden durabilidad solo al soltar la criatura en el mundo y respetan Irrompibilidad.

Existen variantes irrompibles para aldeanos y piglins. Se fabrican colocando el capturador normal en el centro, una estrella del Nether encima y obsidiana llorosa a izquierda, derecha y debajo. La criatura siempre se libera antes de que un capturador normal llegue a romperse.

## Experiencia

El **Almacén de Experiencia** permite guardar o retirar una cantidad concreta de niveles. Si el campo de cantidad queda vacío, el botón correspondiente transfiere todo lo posible. Su capacidad usa el rango positivo completo de `int`, hasta `2.147.483.647` puntos, con operaciones saturadas para impedir desbordamientos.

El depósito expone experiencia líquida mediante la API de transferencia de NeoForge, con una equivalencia de un punto de XP por unidad de fluido. También se puede extraer experiencia líquida del Trocador, del Autotrocador de Aldeanos y de las 21 familias de granjas; el Infusor es un destino y el Almacén es bidireccional.

La experiencia usa las **Tuberías de Fluidos** o **Universales**. La nueva red logística incorpora también tuberías de ítems, gases y energía, sin controlador central ni carga de chunks. Su implementación y validación siguen en desarrollo: [Logística](docs/LOGISTICS.md).

## Control y configuración

Las tuberías se configuran con su **llave inglesa**. Cada conexión admite una mejora que conserva sus reglas: cobre habilita el reparto; hierro, los filtros; oro, los canales; diamante, las reglas avanzadas; netherita aumenta el caudal. Los ítems van al inventario más cercano por defecto; líquidos, gases y energía se reparten por igual. El **Marcador de inventarios** registra un inventario para usarlo en una regla. El antiguo Configurador de Máquinas se ha retirado.

Las máquinas con control de redstone conservan exactamente el progreso al pausarse. En las que admiten comparador, este emite 15 cuando ninguna salida activa puede aceptar más producción y 0 en los demás casos. La granja de interacciones solo tiene pausa manual.

## Sintetizador de materia

El **Sintetizador de materia** (antes Infusor Arcano, ID conservado `arcane_infuser`) usa nueve entradas, una salida y hasta `2.147.483.647` XP. Tiene cámara central, emisores y pantalla superior. En modo libre fabrica al retirar la salida; al bloquear una receta permite fabricación automática y abastecimiento por tolvas/tuberías. Su libro conserva buscador, categorías, filtro de fabricables y colocación de lotes. [Contrato completo](docs/ARCANE_INFUSER.md).

- **Toque del Granjero** (`15.000` XP): libro central, flor de chorus, tótem, estrella del Nether, bloque de netherita y patata, remolacha, trigo y zanahoria en las esquinas.
- **Toque del Minero** (`30.000` XP): libro central, fragmento de eco, tótem, estrella del Nether, bloque de netherita y dos piedras del End más dos catalizadores de sculk en las esquinas.
- **Toque de Seda II** (`75.000` XP): libro con Toque de Seda I en el centro, cuatro fragmentos de eco, un fragmento de amatista, dos huevos de tortuga y una estrella del Nether. Permite recoger bloques especiales que normalmente desaparecen.
- **Nitwit** (`5.000` XP): transforma un aldeano capturado sin empleo, conservando todos sus demás datos y el tipo de capturador.
- **Maquinaria**: granja de entidades, interacciones y otras recetas activas. Las antiguas granjas por familia están retiradas.
- **Toque del Guerrero** (`45.000` XP): crea un libro que evita el desgaste de espadas dentro de todas las granjas de criaturas del mod.
- **Decapitación** (`25.000` XP): usa un Fragmento de la Tormenta en cada esquina para crear un libro aplicable a espadas y hachas.
- **Huevos generadores**: se fabrican en la Mesa de generacion de criaturas,
  no en el sintetizador. Núcleo y huevo de `#trading_cells:arcane_infusion_eggs`, con `1.000/4.000/16.000/64.000`
  XP. Usa huevos registrados o un huevo generico para entidades de mods sin uno.
  Wither y Ender Dragon estan bloqueados por defecto mediante configuracion.

El spawner es renovable mediante una receta tardía con barrotes de hierro, obsidiana, un aliento de dragón y una estrella del Nether.

El depósito mantiene entrada y salida de experiencia líquida y transferencias manuales de niveles. Los ingredientes aparecen dentro de la cámara. REI puede transferirlos; la experiencia se introduce por separado.

## Equipo eléctrico

Condensadores de **128/256 cargas**, recargables en la Estación de tormentas sin cambiar de ranura. Cada fragmento de tormenta aporta **16 cargas**, consumiéndose entero aunque sobre energía. La fulgurita se ha retirado por completo.

El soldador mantiene un haz ramificado: gasta una carga al pulsar y otra cada segundo. La porra añade **10 puntos de daño** con carga. Ambos usan baterías del inventario, sin exigir mano secundaria ni generar rayos del cielo. [Contrato y compatibilidad](docs/ELECTRIC_EQUIPMENT.md).

El Warden suelta un fragmento de eco garantizado cuando lo mata un jugador. Botín puede añadir entre cero y su nivel al fragmento y garantiza entre uno y su nivel de catalizadores de sculk adicionales. Las ciudades antiguas mantienen su obtención habitual de fragmentos de eco.

## Encantamientos

- **Toque del Granjero** evita el desgaste de azadas dentro de ambos Cultivos.
- **Toque del Minero** evita el desgaste de picos dentro de ambas Canteras.
- **Toque de Seda II** sigue siendo `minecraft:silk_touch`, por lo que otros mods lo reconocen como Toque de Seda, y permite recoger los bloques especiales documentados en [`docs/SILK_TOUCH_II.md`](docs/SILK_TOUCH_II.md).
- Los Generadores recogidos muestran una entidad fija en el inventario y pueden convertirse, sin duplicar objetos, en su huevo generador. Las variantes modificadas conservan equipo, efectos, pasajeros y montura.
- Los Generadores de desafio recogidos conservan prueba, criaturas, participantes y variante ominosa; se rearman tras completar la prueba y exigen salir y volver al radio.
- Un comparador instala control por redstone persistente en Generadores normales y de desafio. La senal pausa los normales y bloquea pruebas nuevas sin interrumpir una ya activa; REI documenta ambas variantes.
- Las Arcas son reutilizables con nuevas llaves y no conservan UUID permanentes; la Tarta mantiene sus porciones y los bloques sospechosos conservan contenido, pero reinician el cepillado.
- **Toque del Guerrero** evita el desgaste de espadas dentro de todas las granjas de criaturas del mod.
- **Decapitación I-VI** permite obtener cabezas existentes y aumenta su probabilidad sin depender de Botín. En esqueletos Wither, ambos niveles se suman en una única tirada.
- Decapitación puede combinarse normalmente o subir exactamente un nivel en la mesa de herrería usando el libro o arma encantada y un Fragmento de la Tormenta, hasta nivel VI.
- Decapitación detecta cabezas externas con identificadores convencionales y el tag `#minecraft:skulls` sin inventar resultados inexistentes.
- Los cinco libros están disponibles en creativo y se pueden fabricar mediante Infusión Arcana.
- Eficiencia no modifica los tiempos de Cultivos y Canteras: usan una ranura de velocidad independiente y el nivel de la herramienta.
- Fortuna continúa escalando por encima del nivel vanilla donde la mecánica lo permite.
- Las Canteras permiten combinar Fortuna y Toque de Seda; juntos aumentan selección y cantidad de menas.
- El yunque conserva encantamientos superiores al límite vanilla al añadir otros encantamientos.
- Los niveles XI a CCLV tienen numeración romana y los encantamientos por encima de su máximo normal usan una escala de color azul, verde y magenta.

## Compatibilidad

REI muestra los procesos de criaderos, incubadoras, cultivos, conversión, granjas de hierro, las 21 familias de criaturas, canteras, trueques e infusión arcana, además de las recetas normales. Su integración es opcional y solo se carga en cliente.

Jade tambien es opcional. Cuando esta instalado muestra XP, nivel y progreso de
las maquinas compatibles, ademas del requisito de Toque de Seda II y la entidad
de los generadores especiales. Trading Cells no incluye Jade en su JAR ni lo
exige para iniciar cliente o servidor.

Las listas de profesiones, POI, aspectos de bioma, cultivos, alimentos y niveles de herramienta parten de datos vanilla fijos y se amplían dinámicamente con otros mods. Un elemento externo defectuoso se descarta; si no se puede conservar la ampliación, Trading Cells vuelve a la lista vanilla. Los nombres de profesiones usan el componente registrado por cada mod, incluido More Villagers.

Los datapacks y mods pueden añadir plantas al Cultivo de Aldeanos mediante el tag de ítems `#trading_cells:villager_farmer_plants`. Si no existe un perfil equilibrado propio, el cultivo conserva la tabla de botín del bloque como fuente de sus resultados.

Las granjas de criaturas amplian sus selectores con los tags de sus 21
familias. Los objetos enumerables de sus tablas cargadas reciben filtros
individuales; una tabla externa invalida no elimina las variantes fijas.

La 1.0.0 incluye además un formato público de datapacks para añadir objetivos a
las 21 familias registradas sin sustituir su botín real. Permite elegir
entidad, generador, orden y filtros mediante IDs o tags. El esquema, sus reglas
de fallback y packs de ejemplo están en
[`docs/MOB_FARM_DATAPACKS.md`](docs/MOB_FARM_DATAPACKS.md).
El inventario de criaturas implementadas, candidatas y excluidas esta en
[`docs/MOB_FARM_ROADMAP.md`](docs/MOB_FARM_ROADMAP.md).

### Vulkan y OpenGL

Trading Cells utiliza exclusivamente las capas gráficas neutrales de Blaze3D que proporciona Minecraft 26.2. No llama directamente a OpenGL ni a Vulkan y no fuerza un backend concreto, por lo que funciona con los backends oficiales `OPENGL` y `VULKAN`.

Vulkan sigue siendo experimental en Minecraft 26.2. Si no puede iniciarse, Minecraft puede volver a OpenGL; el backend efectivo debe comprobarse en la línea `Using graphics backend` del registro o en `system_specs` desde la pantalla F3. Esta compatibilidad no supone soporte para el antiguo VulkanMod de terceros.

NeoForge `26.2.0.88` mantiene abierta una [incidencia en su pantalla de carga temprana](https://github.com/neoforged/NeoForge/issues/3230): esa ventana nace con contexto OpenGL y no puede entregarse después a Vulkan. `runClientVulkan` desactiva automáticamente solo esa pantalla en `run/vulkan/config/fml.toml`. En una instalación normal con esa revisión, Vulkan requiere establecer `earlyWindowControl = false` en `config/fml.toml` hasta que NeoForge integre la corrección; OpenGL no necesita este ajuste.

## Configuración

- `timers.*`: duración base de criaderos, incubadoras, cultivos y granja de hierro.
- `production.farmerDamageHoes`: activa el desgaste de azadas, por defecto `true`.
- `production.ironFarmMultiplierBonus`: suma el valor indicado a `x1`, `x2` y `x3`.
- `capturers.durability`: durabilidad común de ambos capturadores, por defecto `10`.
- `timers.villagerInfiniteTrades`: mantiene disponibles las ofertas del Trader y Autotrader.

La referencia completa, límites y efectos exactos están en
[`docs/CONFIGURATION.md`](docs/CONFIGURATION.md).

## Desarrollo

Para desarrollar y ejecutar todas las verificaciones se necesitan Git, JDK 25 y
Python 3.11 o posterior. Instala la dependencia grafica fijada con
`python -m pip install --requirement tools/requirements.txt`.

Los ejemplos usan `./gradlew` en Linux/macOS. En Windows ejecuta el mismo comando
con `gradlew.bat`; no se necesita PowerShell, Bash ni una instalacion global de
Gradle.

```bash
./gradlew clean releaseCheck
```

Este comando compila, valida arquitectura, dominio, recursos, texturas, JAR y
ejecuta los GameTests en servidor. El artefacto publicable queda en
`build/libs/trading_cells-1.0.0.jar`.

Para repetir la comprobacion y registrar el hash, el estado no destructivo del
arbol y el resultado automatizado en `build/reports/release/`:

```bash
./gradlew recordReleaseEvidence
```

`checkReleaseContracts`, incluido en `check`, impide cambiar accidentalmente la
version del mod, IDs de payload, claves NBT persistentes, las 59 recetas o las
versiones publicas del catalogo de granjas durante la linea 1.0.x.

`checkPortability`, tambien incluido en `check`, detecta scripts dependientes del
sistema, rutas absolutas, shells externos, colisiones por mayusculas y residuos
Python versionados.

Cliente de desarrollo con REI:

```bash
./gradlew runClient
```

Clientes de prueba con un backend solicitado explícitamente y directorios de ejecución separados:

```bash
./gradlew runClientVulkan
./gradlew runClientOpenGL
```

Los tres clientes incluyen REI y Jade salvo que se use `-PwithoutRei` o
`-PwithoutJade`. También aceptan `-PquickPlayWorld=<mundo>`:

```bash
./gradlew -PwithoutRei runClientVulkan
./gradlew -PwithoutRei runClientOpenGL
./gradlew -PwithoutJade runClient
```

Servidor dedicado de desarrollo, sin dependencias exclusivas de cliente:

```bash
./gradlew runServer
```

Medicion reproducible de cliente y servidor:

```bash
python tools/performance/run_server_benchmark.py --scenario idle-machines
```

Los escenarios de cliente requieren una plantilla preparada y sellada; consulta
[`tools/performance/README.md`](tools/performance/README.md) para crearla antes de
ejecutar OpenGL o Vulkan.

El perfil cliente puede retirar REI con `--without-rei` y Trading Cells con
`--without-trading-cells` para obtener un control vanilla. Los JFR empiezan
despues del calentamiento y los resultados se guardan en CSV junto con backend,
versiones, mundo, camara y configuracion. La metodologia completa y los cambios
descartados estan en `tools/performance/README.md` y
`tools/performance/RESULTS.md`.

El identificador del mod es `trading_cells`.

La matriz gráfica y sus criterios de revisión están en [`docs/GRAPHICS_BACKENDS.md`](docs/GRAPHICS_BACKENDS.md).
La publicación manual se cierra con [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md)
y el contenido posterior a 1.0.0 permanece separado en [`docs/ROADMAP.md`](docs/ROADMAP.md).
La evidencia de la candidata actual se resume en
[`docs/releases/1.0.0-validation.md`](docs/releases/1.0.0-validation.md).
