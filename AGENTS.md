
# TradingCells - Instrucciones para Codex

## Proyecto

Minecraft 26.3, NeoForge, Java 25 y Gradle.
Las versiones reales se obtienen de gradle.properties.
El código existente es la fuente de verdad.

## Exploración

1. Revisar git status --short antes de editar.
2. Localizar la funcionalidad en PROJECT_INDEX.md.
3. Buscar por símbolo y ruta concreta mediante rg.
4. Leer solo el código, las pruebas y los contratos afectados.
5. Consultar PROJECT_CONTEXT.md por sección relevante;
   no leer todo el historial por defecto.
6. Consultar ARCHITECTURE.md solo si afectan las capas.
7. No recorrer build/, .gradle/, run/, run-server/,
   logs/, artifacts/ ni documentación histórica.
8. Ampliar el contexto si faltan dependencias necesarias.

## Arquitectura y compatibilidad

- Mantener la separación domain/application/adapters/platform.
- No romper límites entre features ni capas compartidas.
- Preservar IDs, NBT, payloads, inventarios y persistencia.
- Preservar recetas y contratos congelados salvo petición.
- Mantener al servidor como autoridad de las mecánicas.
- REI y Jade son opcionales.
- No cargar clases cliente en servidores dedicados.
- Mantener independencia de backends gráficos.
- No repetir optimizaciones rechazadas sin nuevas medidas.
- No inventar métodos o eventos de NeoForge.

## Cambios

- Modificar solo el alcance solicitado y sus dependencias.
- Reutilizar componentes y herramientas existentes.
- Evitar refactorizaciones no relacionadas.
- No regenerar recursos sin comprobar sus fuentes.
- No sobrescribir PNG aprobados automáticamente.
- No modificar gradle.properties sin una necesidad real.

## Verificación

- Consultar la matriz de PROJECT_INDEX.md.
- Ejecutar primero las pruebas dirigidas.
- Ejecutar check antes de finalizar cambios funcionales.
- Ejecutar GameTests pertinentes si cambia el comportamiento
  de Minecraft o la persistencia.
- Reservar clean releaseCheck para validación de publicación.
- Guardar logs completos; mostrar solo errores relevantes.
- No repetir una prueba válida sin cambios posteriores.

## Entrega

Informar brevemente:

- Archivos modificados.
- Funcionalidad implementada.
- Pruebas realizadas y resultado.
- Limitaciones pendientes.

No copiar código o registros extensos en la respuesta.
No declarar pruebas superadas si no se ejecutaron.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.
