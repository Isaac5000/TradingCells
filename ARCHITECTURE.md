# Arquitectura

El mod se organiza por capacidades verticales. Las dependencias de cada feature
apuntan hacia su dominio y sus puertos, mientras que NeoForge queda en los
adaptadores y en `platform/neoforge`.

## Capas

- `domain`: reglas y transiciones puras de Java. No conoce Minecraft, NeoForge,
  configuracion global, NBT ni pantallas.
- `application/port/input`: casos de uso que consumen los adaptadores.
- `application/port/output`: dependencias que necesita la aplicacion, como la
  configuracion propia de la feature.
- `application/service`: implementaciones de los casos de uso.
- `adapters`: bloques, entidades, inventarios, NBT, menus, renderizado,
  registro y eventos de Minecraft.
- `platform/neoforge`: arranque, red, configuracion concreta y composicion.

Una feature no necesita carpetas vacias para todas las capas. Solo se introduce
un servicio de aplicacion cuando existe coordinacion independiente de Minecraft;
las traducciones puras de API permanecen en adaptadores.

## Features

| Feature | Responsabilidad |
| --- | --- |
| `trader` | Trader, Autotrader, ofertas de aldeanos y trueque de piglins. |
| `captures` | Capturadores, datos de entidades capturadas y representacion cliente. |
| `combat` | Decapitacion, Toque del Guerrero, Fragmentos de la Tormenta y eventos globales de combate. |
| `breeders` | Alimentos, coste, tiempo y produccion de criaderos. |
| `incubators` | Crecimiento de entidades capturadas. |
| `farmer` | Cultivos para aldeanos y piglins, fortuna, eficiencia y desgaste de azadas. |
| `quarry` | Canteras para aldeanos y piglins, catalogos de materiales, herramientas y minado profundo. |
| `converter` | Estados de infeccion, curacion y descuento. |
| `ironfarm` | Produccion, multiplicadores y animacion temporal. |
| `mobfarm` | Granja general de entidades, modulos, catalogo dinamico, filtros y experiencia acumulada. |
| `experience` | Calculo y transferencia segura de niveles para el deposito de experiencia. |
| `experiencenetwork` | Grafo de tuberias, conexiones terminales y transferencia atomica de XP. |
| `infusion` | Recetas de infusion, consumo atomico manual y deposito de XP. |
| `machinecontrol` | Configurador, copia de ajustes y seleccion del modo de redstone. |
| `machinecontroller` | Escaneo de maquinas cargadas y presentacion incremental de diagnosticos. |
| `silktouch` | Botin especial de Toque de Seda II y restauracion segura de datos de bloques. |

## Infraestructura de maquinas

`platform/neoforge/machine` publica las fronteras pequenas que necesitan varias
features: `MachineDiagnosticSource`, `MachineConfigurationPort` y la base portable.
Los valores puros de diagnostico y redstone viven en `shared/machines`; no conocen
Minecraft ni dependen de una feature lateral.

`machinecontrol` posee el Configurador y solo invoca el puerto de configuracion.
`machinecontroller` consume el puerto de diagnostico sin conocer clases concretas.
`experiencenetwork` descubre capacidades de fluido de NeoForge en extremos
cargados, pero no importa implementaciones de las maquinas conectadas. Registro,
payloads e integraciones opcionales siguen componiendose desde `platform/neoforge`.
El contrato funcional completo esta en `docs/MACHINE_INFRASTRUCTURE.md`.

## Trader

Todo el contexto de comercio vive en `feature/trader`. Esto elimina la division
anterior entre `tradecages`, `autotrader` y `villagertrading`.

El aldeano manual, el Autotrader y el piglin comparten la frontera de
configuracion `TraderSettingsPort`, el registro de maquinas y las reglas de
comercio que realmente son comunes. Sus mecanicas no se mezclan:

- `VillagerTraderUseCase` controla el comercio manual.
- `AutotraderUseCase` controla seleccion, ofertas y experiencia automatica.
- `PiglinBarterUseCase` controla el ciclo temporizado de trueque.

De este modo el piglin pertenece a la misma capacidad funcional sin forzarlo a
usar conceptos de ofertas de aldeano.

## Captures

`captures` es una feature independiente porque registra items propios, captura y
libera entidades y publica una frontera usada por varias maquinas.

Su capa `application` coordina la durabilidad configurable mediante
`CaptureUseCase`, `CaptureSettingsPort` y `CaptureService`. Las operaciones que
traducen `ItemStack`, NBT y entidades de Minecraft permanecen en adaptadores. La
interaccion del jugador vive en `CaptureInteractionAdapter`, y otras features
solo pueden usar `CapturedMobKind` o `CapturedMobStackAdapter`.

## Toque de Seda II

`silktouch` conserva el encantamiento vanilla `minecraft:silk_touch`: no registra
un encantamiento paralelo ni modifica su nivel maximo. El Infusor genera de
forma controlada un libro con nivel 2 y el adaptador de yunque conserva ese nivel
al aplicarlo. Por ello, cualquier mod que compruebe Toque de Seda sigue viendo el
encantamiento estandar.

Los bloques admitidos y su herramienta se definen mediante tags propios. Un
modificador global agrega el bloque sin sustituir tablas de botin de Minecraft,
lo que mantiene la composicion con datapacks y otros modificadores. Los datos de
Block Entities sensibles se copian al componente del objeto y se restauran al
colocarlo; el marcador interno limita esta excepcion a objetos producidos por
esta mecanica.

Los mismos tags se enlazan con las clases de herramienta vanilla. Un adaptador
de velocidad mantiene lentos los bloques especiales cuando la herramienta no
lleva el nivel II, aunque sea de la clase correcta. De este modo se obtiene la
respuesta eficiente del pico o la pala solo cuando el bloque tambien podra
recogerse.

`trading_cells.mixins.json` contiene extensiones comunes para el generador de
prueba, la boveda y dos puntos protegidos de objetos vanilla. Los mixins de
objetos solo eliminan la advertencia de operador en generadores y huevos que
portan el marcador confiable creado por Toque de Seda II, y desvían el uso del
huevo para restaurar una jerarquia guardada. Los demas objetos NBT conservan la
proteccion vanilla. Los mixins de prueba y boveda existen porque sus
transiciones internas, temporizadores y registro privado de jugadores no tienen
eventos equivalentes en NeoForge.

La conversion de generador a huevo selecciona de forma determinista una entidad
representativa, pero almacena el arbol completo. Las entidades con modelo de
armadura humanoide se declaran mediante un tag ampliable; la alternativa es el
jinete superior. El render especial de inventario es exclusivamente cliente y
presenta una entidad inmovil dentro del modelo vanilla del generador.

## Shared Kernel

`TimedProcess` no es una feature registrable. Vive en
`shared/machines/domain/model` como regla pura compartida por procesos
temporizados.

`MobFarmCycleRules` y `VanillaSwordTier` viven en
`shared/mobfarm/domain/model`. Son reglas puras compartidas por las familias de
granjas para duracion, bajas simuladas, progreso y filtros; no conocen entidades,
inventarios, botin ni NeoForge. `MobFarmWeaponSnapshot` centraliza en la plataforma
la lectura inmutable de tier y encantamientos. Las granjas de esqueletos,
zombis, saqueadores y creepers conservan sus Block Entities, pantallas y reglas
de botin propias y componen este nucleo, sin heredar de una Block Entity comun.

`combat` es una feature compartida con frontera publica deliberada. Otras
features solo acceden a sus reglas de dominio o a `adapters/api`; el registro,
los eventos globales y la receta de herreria permanecen dentro de `combat`.

La antigua interfaz global `MachineSettingsPort` se elimino. Cada feature posee
su puerto de configuracion (`BreederSettingsPort`, `CaptureSettingsPort`,
`ConverterSettingsPort`, `FarmerSettingsPort`, `IncubatorSettingsPort`, `IronFarmSettingsPort` o
`TraderSettingsPort`). El deposito de experiencia no necesita configuracion
externa: su caso de uso puro se compone mediante `ExperienceStorageUseCase`.
NeoForge agrega los puertos configurables mediante `FeatureSettings` solo en la
capa de composicion.

## Composicion

Los adaptadores dependen de puertos de entrada, nunca de servicios concretos.
`FeatureComposition` es el unico lugar que construye servicios. La
configuracion concreta se instala con `FeatureSettingsProvider` y
`NeoForgeFeatureSettingsAdapter`.

Los IDs de registro, claves NBT y recursos no dependen del paquete Java, por lo
que la reorganizacion conserva la compatibilidad de mundos.

## Catalogos de granjas

`MobFarmCatalog` crea un snapshot inmutable por revision durante las recargas de
datos. Instala primero objetivos vanilla, despues descubre entidades por tags y
por ultimo aplica descriptores
`trading_cells/mob_farm_target` de esquema 1. Los ciclos solo leen el snapshot;
no recorren registros ni expanden tags por tick.

Un descriptor controla descubrimiento, icono, orden y filtros. La tabla de
botin cargada de la entidad sigue siendo la unica fuente de resultados. Un
archivo invalido se descarta de forma aislada y un fallo global restaura el
catalogo vanilla. El servidor sincroniza IDs concretos mediante el payload
versionado `mob_farm_catalog_sync`; una familia nueva sigue requiriendo codigo y
un bloque propio.

Las cuatro familias publicadas son `skeleton`, `zombie`, `raider` y `creeper`.
Las variantes ominosa y cargada son objetivos sinteticos propiedad de sus
features; el catalogo publico amplia entidades registradas reales mediante tags
o descriptores. `combat` publica el Fragmento de la Tormenta mediante una
fachada `adapters/api`, evitando que la Granja de Creepers dependa de su registro
interno.

## Renderizado neutral

El cliente se apoya en las abstracciones de Blaze3D y Minecraft 26.2:
`SubmitNodeCollector`, estados de renderizado, `GuiGraphicsExtractor`,
`RenderType`, `VertexConsumer`, modelos vanilla y `RenderPipelines`. Blaze3D
elige internamente entre los backends oficiales Vulkan y OpenGL.

El codigo del mod no puede importar ni referenciar implementaciones concretas
de `org.lwjgl.opengl`, `org.lwjgl.vulkan`, `com.mojang.blaze3d.opengl` o
`com.mojang.blaze3d.vulkan`, ni las clases `GlStateManager`, `GlDevice` o
`VulkanDevice`. GLFW sigue permitido exclusivamente para entrada de teclado y
raton. No se mantienen ramas de renderizado distintas por backend.

Los perfiles cliente usan el source set vacio `developmentClient`, cuyo
classpath reutiliza la salida de `main` y agrega las dependencias cliente
opcionales. Los perfiles servidor siguen usando `main`, de modo que REI,
Architectury y Cloth Config no pueden filtrarse al servidor dedicado ni al JAR.
El perfil Vulkan prepara su configuracion local desactivando la ventana temprana
OpenGL de NeoForge `26.2.0.88`, como solucion temporal a la incidencia
upstream `NeoForge#3230`; no existe ningun workaround dentro del codigo del mod.

Jade constituye otra integracion opcional. Sus clases se concentran en
`platform/neoforge/integration/jade`, la API es `compileOnly` y el descriptor la
declara opcional. El proveedor comun envia solo XP y progreso usados por el
tooltip; la presentacion permanece en el cliente. El servidor arranca sin Jade,
y el perfil de desarrollo puede retirarlo con `-PwithoutJade`.

El source set `performanceClient` contiene un grabador de desarrollo separado.
Solo se registra cuando se proporciona `performanceClientOutput`, puede excluir
Trading Cells para medir un control vanilla y nunca forma parte de `main` ni del
JAR publicado. La grabacion JFR y las capturas se realizan despues del
calentamiento para no contaminar las metricas con la carga inicial.

## Verificacion

`checkArchitecture` comprueba:

- correspondencia entre carpetas y paquetes;
- pureza de dominio y aplicacion;
- ausencia de dependencias laterales entre features;
- acceso a `captures` solo por su frontera publica;
- acceso a `combat` solo por dominio o su API publica;
- independencia del shared kernel;
- composicion de servicios exclusivamente en `FeatureComposition`;
- ausencia de GameTests de produccion en `src/main`.

`checkGraphicsBackendIndependence` recorre todo `src/main/java`, rechaza
referencias a implementaciones graficas concretas y forma parte de `check`.

`verifyDomainRules` ejecuta las comprobaciones puras del dominio.

`checkProjectResources` analiza JSON con deteccion de claves duplicadas,
traducciones ingles/espanol, modelos, texturas, recetas y directorios de datos.
`checkPublishedJarContents` inspecciona el artefacto final y rechaza GameTests,
herramientas y recursos obsoletos. Ambas tareas forman parte de `check`.

`checkPortability` rechaza herramientas auxiliares especificas de un sistema,
shells externos, rutas absolutas operativas, APIs de proceso Windows sin proteger,
colisiones de mayusculas/minusculas y bytecode Python versionado. Tambien forma
parte de `check`; la CI lo ejecuta en Ubuntu, Windows y macOS.

Los GameTests viven en `src/gameTest`, fuera del JAR. Una registradora minima
recoge suites por feature y fixtures compartidos; cada prueba publica un contrato
de comportamiento y no depende de la ubicacion interna del metodo probado.
Verifican el round-trip NBT de todas las Block Entities registradas, contratos de
fluido XP, limites manuales del Infusor, codecs y valores invalidos de payload,
ciclos sin botin de ambas familias de granjas, capacidad parcial de salidas,
matriz de Fortuna, capacidades laterales, Toque de Seda II y generadores
conservados.
`releaseCheck` ejecuta `check`, el servidor de GameTests y comprueba que solo
exista un JAR publicable.

`checkReleaseContracts` congela para la linea 1.0.0 la version del mod, los IDs
de payload, las claves NBT persistentes, el contenido canonico de las recetas y
las versiones publicas del descriptor y del catalogo de granjas.
`recordReleaseEvidence` inventaria el arbol sin tocarlo y
registra hash y tamano del JAR bajo `build/reports/release/`.

El escalado de textos de una sola linea usa `FittedTextRenderer`. Las granjas
mantienen sus paletas y layouts independientes, pero comparten la formula de
ajuste para traducciones o nombres largos aportados por otros mods.

Las pruebas de equivalencia y los umbrales de rendimiento estan documentados en
`tools/performance/README.md`. Un cambio local requiere al menos un 3 % repetible;
uno estructural, un 10 %. Una regresion primaria superior al 1 % descarta la
hipotesis. Cada comparacion exige la misma huella de plantilla para impedir que
un mundo o estado distinto falsee el resultado.
