# Backends graficos

Trading Cells es compatible con los backends oficiales OpenGL y Vulkan de
Minecraft 26.2. El mod no selecciona un backend durante el juego ni incluye una
implementacion grafica propia: Blaze3D conserva esa responsabilidad.

## Perfiles de desarrollo

| Perfil | Directorio | Backend solicitado |
| --- | --- | --- |
| `runClient` | `run/` | Predeterminado de Minecraft |
| `runClientVulkan` | `run/vulkan/` | `VULKAN` |
| `runClientOpenGL` | `run/opengl/` | `OPENGL` |

Los tres perfiles cargan REI de forma predeterminada y aceptan
`-PquickPlayWorld=<mundo>`. Para probar sin REI:

```bash
./gradlew -PwithoutRei runClientVulkan
./gradlew -PwithoutRei runClientOpenGL
```

Un source set de ejecucion llamado `developmentClient` agrega REI, Architectury
y Cloth Config solo a los perfiles cliente. El servidor y el artefacto publicado
siguen usando `main`, por lo que esas dependencias cliente no se filtran al lado
servidor.

La opcion solicitada no demuestra por si sola que se haya iniciado ese backend.
Hay que comprobar la linea `Using graphics backend` del registro final o el
campo `system_specs` de la pantalla F3. Vulkan es experimental y Minecraft puede
usar OpenGL como fallback si el equipo o el controlador no son compatibles.

### Ventana temprana de NeoForge

NeoForge `26.2.0.55-beta` tiene abierta la incidencia
[`NeoForge#3230`](https://github.com/neoforged/NeoForge/issues/3230): su pantalla
temprana crea una ventana con contexto OpenGL y Minecraft no puede reutilizarla
para Vulkan. `runClientVulkan` ejecuta `prepareVulkanFmlConfig` y establece
`earlyWindowControl = false` en el archivo local
`run/vulkan/config/fml.toml`. El ajuste solo afecta a ese entorno ignorado por
Git y no entra en el JAR.

En una instalacion normal basada en `.55-beta`, el jugador debe aplicar el mismo
valor en `config/fml.toml` mientras la correccion upstream siga pendiente.

## Frontera de codigo

Los renderizadores y pantallas deben usar APIs neutrales como
`SubmitNodeCollector`, estados de renderizado, `GuiGraphicsExtractor`,
`RenderType`, `VertexConsumer`, modelos vanilla y `RenderPipelines`.

La tarea `checkGraphicsBackendIndependence`, incluida en `check`, rechaza
referencias directas a LWJGL OpenGL/Vulkan y a las implementaciones internas de
backend de Blaze3D. GLFW permanece permitido para teclado y raton.

## Matriz visual

En ambos backends se revisan:

- renderizadores de todas las maquinas y sus entidades internas;
- capturadores en inventario, manos y marcos;
- bloques minados, Infusor Arcano y orbe del Almacen de Experiencia;
- todas las pantallas y categorias REI;
- scissor, desplegables, tooltips, glint, transparencias, profundidad y textos.

Son admisibles diferencias menores de rasterizado. No lo son geometria ausente
o duplicada, colores perceptiblemente distintos, transparencias incorrectas,
solapes ni recortes. El servidor dedicado se prueba aparte para confirmar que
ninguna utilidad grafica se carga en el lado servidor.

El objetivo comprende los backends incluidos en Minecraft 26.2; no incluye el
antiguo VulkanMod de terceros.
