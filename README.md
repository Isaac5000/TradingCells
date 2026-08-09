# Trading Cells

Trading Cells es un mod para Minecraft 26.2 con NeoForge que automatiza el trabajo de aldeanos y piglins mediante máquinas portátiles. Conserva los datos de las criaturas, sus ofertas, inventarios y progresos cuando una máquina se rompe y vuelve a colocarse.

## Requisitos

- Minecraft `26.2.0`
- NeoForge `26.2.0.57` o posterior compatible con Minecraft 26.2
- Java `25`
- Roughly Enough Items `26.2.820` o posterior es opcional

La versión actual del mod es `1.0.0`.

## Máquinas de aldeanos

- **Trocador de Aldeanos**: conserva un aldeano adulto, su POI, ofertas, descuentos y experiencia de comercio. Admite intercambio masivo con Mayús.
- **Autotrocador de Aldeanos**: ejecuta automáticamente la oferta elegida usando entradas y salidas automatizables.
- **Criadero de Aldeanos**: consume alimentos configurados y capturadores vacíos para producir aldeanos bebé.
- **Incubador de Aldeanos**: convierte un aldeano bebé capturado en adulto.
- **Cultivo de Aldeanos**: procesa cultivos con dieciocho salidas; el nivel de la azada, Eficiencia y Fortuna afectan al ciclo.
- **Convertidor de Aldeanos**: zombifica y cura sin perder ofertas, y conserva los descuentos de curación.
- **Granja de Hierro**: produce hierro con multiplicadores base `x1`, `x2` y `x3` según los aldeanos instalados.
- **Cantera de Aldeanos**: extrae materiales del Overworld. El pico, su nivel, Eficiencia, Fortuna y Toque de Seda afectan al resultado.

## Máquinas de piglins

- **Trocador de Piglins**: automatiza la tabla de trueque vanilla.
- **Trocador de Piglins de Netherite**: añade ocho salidas, cinco niveles de mejora y filtros en el nivel de Netherite.
- **Criadero de Piglins** e **Incubador de Piglins**: reproducen y hacen crecer piglins capturados.
- **Cultivo de Piglins**: cultiva vegetación del Nether con dieciocho salidas; Fortuna aumenta cantidades y probabilidades especiales.
- **Cantera de Piglins**: extrae materiales del Nether y ofrece minado profundo con las mejoras compatibles.

Las máquinas de comercio y trabajo rechazan aldeanos o piglins bebé cuando su función requiere una criatura adulta.

## Capturadores

Los capturadores guardan la entidad completa y se apilan de uno en uno. Tienen diez liberaciones por defecto, pierden durabilidad solo al soltar la criatura en el mundo y respetan Irrompibilidad.

Existen variantes irrompibles para aldeanos y piglins. Se fabrican colocando el capturador normal en el centro, una estrella del Nether encima y obsidiana llorosa a izquierda, derecha y debajo. La criatura siempre se libera antes de que un capturador normal llegue a romperse.

## Experiencia

El **Almacén de Experiencia** permite guardar o retirar una cantidad concreta de niveles. Si el campo de cantidad queda vacío, el botón correspondiente transfiere todo lo posible. Su capacidad usa el rango positivo completo de `int`, hasta `2.147.483.647` puntos, con operaciones saturadas para impedir desbordamientos.

El depósito expone experiencia líquida mediante la API de transferencia de NeoForge, con una equivalencia de un punto de XP por unidad de fluido. También se puede extraer experiencia líquida del Trocador y del Autotrocador de Aldeanos; estas dos máquinas son exclusivamente de salida.

## Infusor Arcano

El **Infusor Arcano** fabrica los libros de Toque del Granjero y Toque del Minero. Dispone de cinco entradas en cruz, una salida independiente y un depósito de hasta `2.147.483.647` puntos de experiencia. Toque del Granjero consume `15.000` puntos y Toque del Minero `30.000`. La operación es instantánea, procesa como máximo una receta por tick y solo consume los recursos cuando la receta completa, la experiencia y el espacio de salida están disponibles.

- **Toque del Granjero**: libro en el centro, 16 alientos de dragón arriba, tótem de la inmortalidad a la izquierda, estrella del Nether a la derecha y bloque de netherita abajo.
- **Toque del Minero**: libro en el centro, 1 fragmento de eco arriba, tótem de la inmortalidad a la izquierda, estrella del Nether a la derecha y bloque de netherita abajo.

Las cinco entradas aceptan automatización desde cualquier cara y las tuberías o tolvas solo pueden extraer el resultado. El depósito admite entrada y salida de experiencia líquida, además de transferencias manuales de niveles del jugador. La máquina conserva inventario y experiencia al romperse y recolocarse.

El Warden suelta un fragmento de eco garantizado cuando lo mata un jugador. Saqueo añade aleatoriamente entre cero y su nivel sin sustituir el botín original. Las ciudades antiguas mantienen su obtención habitual de fragmentos de eco.

## Encantamientos

- **Toque del Granjero** evita el desgaste de azadas dentro de ambos Cultivos.
- **Toque del Minero** evita el desgaste de picos dentro de ambas Canteras.
- Los dos libros están disponibles en creativo y se pueden fabricar mediante Infusión Arcana.
- Eficiencia se limita funcionalmente al nivel V dentro de Cultivos y Canteras.
- Fortuna continúa escalando por encima del nivel vanilla donde la mecánica lo permite.
- Las Canteras permiten combinar Fortuna y Toque de Seda; juntos aumentan selección y cantidad de menas.
- El yunque conserva encantamientos superiores al límite vanilla al añadir otros encantamientos.
- Los niveles XI a CCLV tienen numeración romana y los encantamientos por encima de su máximo normal usan una escala de color azul, verde y magenta.

## Compatibilidad

REI muestra los procesos de criaderos, incubadoras, cultivos, conversión, granja de hierro, canteras, trueques e infusión arcana, además de las recetas normales. Su integración es opcional y solo se carga en cliente.

Las listas de profesiones, POI, aspectos de bioma, cultivos, alimentos y niveles de herramienta parten de datos vanilla fijos y se amplían dinámicamente con otros mods. Un elemento externo defectuoso se descarta; si no se puede conservar la ampliación, Trading Cells vuelve a la lista vanilla. Los nombres de profesiones usan el componente registrado por cada mod, incluido More Villagers.

### Vulkan y OpenGL

Trading Cells utiliza exclusivamente las capas gráficas neutrales de Blaze3D que proporciona Minecraft 26.2. No llama directamente a OpenGL ni a Vulkan y no fuerza un backend concreto, por lo que funciona con los backends oficiales `OPENGL` y `VULKAN`.

Vulkan sigue siendo experimental en Minecraft 26.2. Si no puede iniciarse, Minecraft puede volver a OpenGL; el backend efectivo debe comprobarse en la línea `Using graphics backend` del registro o en `system_specs` desde la pantalla F3. Esta compatibilidad no supone soporte para el antiguo VulkanMod de terceros.

NeoForge `26.2.0.57` mantiene abierta una [incidencia en su pantalla de carga temprana](https://github.com/neoforged/NeoForge/issues/3230): esa ventana nace con contexto OpenGL y no puede entregarse después a Vulkan. `runClientVulkan` desactiva automáticamente solo esa pantalla en `run/vulkan/config/fml.toml`. En una instalación normal con esa revisión, Vulkan requiere establecer `earlyWindowControl = false` en `config/fml.toml` hasta que NeoForge integre la corrección; OpenGL no necesita este ajuste.

## Configuración

- `timers.*`: duración base de criaderos, incubadoras, cultivos y granja de hierro.
- `production.farmerDamageHoes`: activa el desgaste de azadas, por defecto `true`.
- `production.ironFarmMultiplierBonus`: suma el valor indicado a `x1`, `x2` y `x3`.
- `capturers.durability`: durabilidad común de ambos capturadores, por defecto `10`.
- `timers.villagerInfiniteTrades`: mantiene disponibles las ofertas del Trader y Autotrader.

## Desarrollo

```bash
./gradlew clean build verifyDomainRules checkArchitecture checkGraphicsBackendIndependence
```

Cliente de desarrollo con REI:

```bash
./gradlew runClient
```

Clientes de prueba con un backend solicitado explícitamente y directorios de ejecución separados:

```bash
./gradlew runClientVulkan
./gradlew runClientOpenGL
```

Los tres clientes incluyen REI salvo que se use `-PwithoutRei`. También aceptan `-PquickPlayWorld=<mundo>`:

```bash
./gradlew -PwithoutRei runClientVulkan
./gradlew -PwithoutRei runClientOpenGL
```

Servidor dedicado de desarrollo, sin dependencias exclusivas de cliente:

```bash
./gradlew runServer
```

Medicion reproducible de cliente y servidor:

```bash
python tools/performance/run_client_benchmark.py --backend opengl --scenario vanilla-control
python tools/performance/run_server_benchmark.py --scenario idle-machines
```

El perfil cliente puede retirar REI con `--without-rei` y Trading Cells con
`--without-trading-cells` para obtener un control vanilla. Los JFR empiezan
despues del calentamiento y los resultados se guardan en CSV junto con backend,
versiones, mundo, camara y configuracion. La metodologia completa y los cambios
descartados estan en `tools/performance/README.md` y
`tools/performance/RESULTS.md`.

El identificador del mod es `trading_cells`.

La matriz gráfica y sus criterios de revisión están en [`docs/GRAPHICS_BACKENDS.md`](docs/GRAPHICS_BACKENDS.md).
