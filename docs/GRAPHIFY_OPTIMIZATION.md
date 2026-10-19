# Graphify: revision de logistica

Fecha: 2026-10-18. Minecraft 26.3.0, NeoForge 26.3.0.88, Java 25.

## Alcance y limites

Analisis AST local de 47 archivos de `feature/logistics`: 837 nodos,
2.708 aristas y 43 comunidades. Sin subagentes, servicios remotos ni tokens
de extraccion LLM. No representa una auditoria de todas las features.
Mapa e informe: `graphify-out/graph.html` y `graphify-out/GRAPH_REPORT.md`.
El grafo corresponde al codigo anterior a la optimizacion descrita debajo.

Nodos centrales: LogisticsResourceType, LogisticsNetworkManager y
LogisticsPipeBlockEntity. La relacion entre el listado de inventarios,
LogisticsResourceSnapshot y LogisticsComponentData llevo a revisar las huellas.
El grafo no demuestra por si solo coste de ejecucion ni codigo muerto.

Diagnostico del extractor: 231 referencias sin destino en este corpus,
5 autorreferencias y 168 relaciones paralelas colapsadas al construir DiGraph.
Por eso las conclusiones se contrastaron con los fuentes.

Graphify estima unas 1.621 fichas por consulta frente a 55.800 del corpus
completo: 34,4 veces menos contexto en sus consultas de ejemplo. Es una
estimacion del propio programa, no el ahorro real total de esta sesion.

## Cambio medido

`LogisticsComponentData.serialized` devuelve directamente `{}` cuando el
DataComponentPatch esta vacio. Evita codec, NBT y conversion a texto en ese
caso, sin cache persistente ni cambios de identidad, filtros o paquetes.
Los componentes modificados y eliminados siguen pasando por el codec.

Medicion de la implementacion real: seis calentamientos, nueve rondas de
200.000 llamadas, orden baseline/candidato alternado, mismo proceso/JVM.
Medianas: 6,144 ms frente a 0,856 ms; reduccion local del 86,07 %.
Repeticion independiente con el comando documentado: 4,962 ms frente a
0,787 ms; reduccion local del 84,15 %.
No es una medicion de FPS, MSPT ni de redes completas. No se atribuye mejora
global ni se han aplicado cambios estructurales al gestor de redes.

Repeticion desde la raiz con JDK 25:

```text
./gradlew -I tools/performance/fingerprint-benchmark.init.gradle runFingerprintProbe
```

## Validacion

- Contrato ampliado: vacio canonico, builder vacio, componente eliminado y
  componentes modificados producen exactamente la serializacion vanilla.
- `check runGameTestServer`: correcto, 170 GameTests obligatorios.
- Sin cambios de IDs, NBT, recetas, PNG, capacidades ni dependencias.
- Registros completos locales conservados en `graphify-out/validation/`.
- `artifacts/` ya no se necesita para este trabajo. El usuario autorizo borrarlo,
  pero el entorno rechazo la operacion; la carpeta permanece intacta.
- No se ha ejecutado `clean releaseCheck`; no es un cierre de publicacion.
