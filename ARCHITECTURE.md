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
| `breeders` | Alimentos, coste, tiempo y produccion de criaderos. |
| `incubators` | Crecimiento de entidades capturadas. |
| `farmer` | Cultivos para aldeanos y piglins, fortuna, eficiencia y desgaste de azadas. |
| `quarry` | Canteras para aldeanos y piglins, catalogos de materiales, herramientas y minado profundo. |
| `converter` | Estados de infeccion, curacion y descuento. |
| `ironfarm` | Produccion, multiplicadores y animacion temporal. |
| `experience` | Calculo y transferencia segura de niveles para el deposito de experiencia. |
| `infusion` | Recetas de infusion, consumo atomico, modos manual/automatico y deposito de XP. |

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

## Shared Kernel

`TimedProcess` no es una feature registrable. Vive en
`shared/machines/domain/model` como regla pura compartida por procesos
temporizados.

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
OpenGL de NeoForge `26.2.0.57`, como solucion temporal a la incidencia
upstream `NeoForge#3230`; no existe ningun workaround dentro del codigo del mod.

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
- independencia del shared kernel;
- composicion de servicios exclusivamente en `FeatureComposition`;
- ausencia de GameTests de produccion en `src/main`.

`checkGraphicsBackendIndependence` recorre todo `src/main/java`, rechaza
referencias a implementaciones graficas concretas y forma parte de `check`.

`verifyDomainRules` ejecuta las comprobaciones puras del dominio.

Las pruebas de equivalencia y los umbrales de rendimiento estan documentados en
`tools/performance/README.md`. Un cambio local requiere al menos un 3 % repetible;
uno estructural, un 10 %. Una regresion primaria superior al 1 % descarta la
hipotesis.
