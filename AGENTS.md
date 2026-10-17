# Guia breve para agentes

## Proyecto

1. Lee `PROJECT_CONTEXT.md` para conocer invariantes, decisiones descartadas y estado.
2. Usa `PROJECT_INDEX.md` solo como enrutador y abre la fila de la feature afectada.
3. Lee la documentacion enlazada solo si el cambio toca ese contrato.

Busca con `rg` dentro de rutas concretas. No indexes `.gradle/`, `build/`, `logs/`,
`run/` ni `run-server/`; son caches, resultados o mundos locales. Evita releer
JSON de idiomas, catalogos completos y resultados historicos salvo que la tarea
los afecte.

Antes de editar, revisa `git status` y conserva cambios ajenos. Mantiene IDs, NBT,
payloads y mecanicas salvo peticion explicita. Valida primero la tarea dirigida
del indice y termina con `check`; usa `releaseCheck` para cambios publicables.

Mod de Minecraft Java con NeoForge y Gradle.
Obtener las versiones reales de gradle.properties y build.gradle.
El código existente es la fuente de verdad de la arquitectura.

## Contexto

- README.md: descripción funcional.
- docs/ARCHITECTURE.md: arquitectura, solo cuando sea relevante.
- docs/DECISIONS.md: decisiones técnicas.
- docs/features/: requisitos de sistemas concretos.
- tasks/CURRENT.md: estado de tareas largas, si existe.

## Exploración

- Localizar archivos con búsquedas por nombre, símbolo o paquete.
- Leer solo archivos y fragmentos relacionados con la tarea.
- No recorrer build/, run/, .gradle/ ni archivos generados.
- No imprimir archivos completos cuando basta un fragmento.
- No volver a leer contenido ya disponible sin motivo.

## Implementación

- Respetar IDs, registros y convenciones existentes.
- Preferir modificaciones pequeñas y reutilizar código.
- Evitar refactorizaciones ajenas a la petición.
- No inventar métodos, eventos ni firmas de NeoForge.
- Verificar APIs dudosas contra la versión real del proyecto.
- No introducir dependencias obligatorias innecesarias.
- Mantener cliente y servidor correctamente separados.

## Verificación

- Java: compilar los cambios.
- Recursos y datos: validar las rutas y el procesamiento.
- Lógica compleja: ejecutar pruebas específicas si existen.
- Rendering y worldgen: identificar comprobaciones en juego.
- Corregir fallos causados por los cambios realizados.
- No repetir una prueba exitosa sin nuevos cambios.

## Finalización

- No declarar éxito sin comprobación suficiente.
- Resumir cambios, archivos afectados y pruebas ejecutadas.
- Indicar fallos pendientes y comprobaciones no realizadas.
- Evitar explicaciones largas o copiar código completo.
- Actualizar documentación solo cuando cambie su contenido.
