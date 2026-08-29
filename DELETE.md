# Auditoria de archivos prescindibles

Registro actualizado el `2026-08-29`. Solo se eliminan rutas versionadas con
reemplazo y ausencia de consumidores comprobados.

## Eliminados de forma segura

| Ruta | Motivo | Reemplazo |
| --- | --- | --- |
| `tools/performance/verify_output_inserter.ps1` | verificador Windows duplicado | `run_java_benchmark.py output-inserter` |
| `tools/performance/verify_high_level_tooltip.ps1` | verificador Windows duplicado | `run_java_benchmark.py high-level-tooltip` |
| `tools/performance/verify_autotrader_readiness.ps1` | verificador Windows duplicado | `run_java_benchmark.py autotrader-readiness` |
| `tools/performance/summarize_jfr.ps1` | resumen JFR Windows-only | `summarize_jfr.py` |
| `tools/release/__pycache__/*.pyc` | bytecode generado versionado | fuente Python y `.gitignore` |
| `tools/generate_piglin_capturer.py` | sin consumidores y no reproduce el PNG actual | textura versionada actual |
| `tools/generate_villager_capturer.py` | sin consumidores y no reproduce el PNG actual | textura versionada actual |

## Generados locales regenerables

No son fuentes y estan ignorados: `.gradle/`, `build/`, `logs/`, caches e informes
de `run/` y `run-server/`, `__pycache__/`, `*.pyc`, `out/` y `bin/`. No se borran
automaticamente porque los mundos y resultados locales pueden servir para regresion.

## Candidatos a eliminacion

No queda ningun candidato versionado con duda razonable. Los dos generadores de
capturadores se probaron desde otro directorio: ambos cambiaron los PNG actuales,
por lo que se restauraron las texturas y se eliminaron los generadores obsoletos.

## Conservados deliberadamente

- `gradlew` y `gradlew.bat`: launchers oficiales necesarios para Unix y Windows;
  no son implementaciones duplicadas de logica del proyecto.
- `gradle/wrapper/`: build reproducible.
- Python bajo `tools/`: validacion grafica, recursos, publicacion, plantillas y
  rendimiento; es portable y evita una migracion artificial a Java.
- `tools/generate_enchantment_levels.py`: fuente determinista y portable de las
  traducciones romanas 11-255; sigue siendo util al portar recursos.
- `tools/performance/results/` y `baselines/`: evidencia de optimizaciones aceptadas
  y rechazadas que evita repetir regresiones.
- `run/saves/`, `run/world/`, `run-server/world/` y configuraciones locales: estan
  ignorados y pueden contener fixtures manuales; decidir su limpieza fuera de Git.
- Documentos de `docs/`: cada uno mantiene un contrato funcional, de compatibilidad,
  publicacion o rendimiento distinto. `PROJECT_CONTEXT.md` solo los enruta.

No quedan scripts PowerShell, Bash o CMD propios. Tampoco se encontro codigo
residual de features eliminadas ni otro archivo versionado eliminable con certeza.
