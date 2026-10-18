
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
