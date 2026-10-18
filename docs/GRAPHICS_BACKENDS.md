# Backends graficos

Trading Cells es compatible con los backends oficiales OpenGL y Vulkan de
Minecraft 26.3. El mod no selecciona un backend durante el juego ni incluye una
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

El perfil de desarrollo `runClientVulkan` ejecuta `prepareVulkanFmlConfig` y establece
`earlyWindowControl = false` en el archivo local
`run/vulkan/config/fml.toml`. El ajuste solo afecta a ese entorno ignorado por
Git y no entra en el JAR.

Este ajuste se conserva de la migracion anterior. La validacion local de 26.3
usa ese perfil; no demuestra que una instalacion normal necesite desactivar
la ventana temprana.

## Frontera de codigo

Los renderizadores y pantallas deben usar APIs neutrales como
`SubmitNodeCollector`, estados de renderizado, `GuiGraphicsExtractor`,
`RenderType`, `VertexConsumer`, modelos vanilla y `RenderPipelines`.

La tarea `checkGraphicsBackendIndependence`, incluida en `check`, rechaza
referencias directas a LWJGL OpenGL/Vulkan y a las implementaciones internas de
backend de Blaze3D. Teclado y raton usan `InputConstants`, que en 26.3 expone
los codigos SDL3 de Minecraft. No se utilizan codigos GLFW ni numeros literales
para comparar botones fisicos del raton.

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

El objetivo comprende los backends incluidos en Minecraft 26.3; no incluye el
antiguo VulkanMod de terceros.

## Medicion automatizada

`tools/performance/run_client_benchmark.py` solicita y verifica el backend
efectivo, fija mundo/camara/opciones, inicia JFR despues del calentamiento y toma
la captura en un fotograma posterior. Admite las cuatro combinaciones
OpenGL/Vulkan con/sin REI y un control `--without-trading-cells`.

Cada escenario usa una plantilla registrada con
`prepare_template_manifest.py`. El comparador exige la misma huella SHA-256 de
mundo y estado en baseline y candidato; cambiar de plantilla invalida la pareja
de resultados.

Las comparaciones usan `compare_client_results.py`. El comparador comprueba
metadatos, media, p95, CPU, asignaciones, memoria, trafico y capturas. No aplica
tolerancias visuales salvo que se indiquen explicitamente.
