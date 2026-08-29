# Herramientas

Indice para evitar recorrer `tools/` completo. Todas las herramientas conservadas
son Python 3.11+ portable y resuelven rutas desde `__file__` o argumentos.

## Invocadas por Gradle

| Script | Tarea / responsabilidad |
| --- | --- |
| `check_portability.py` | `checkPortability`; SO, rutas, shells, case y residuos |
| `generate_machine_gui.py --check` | `checkMachineGuiTextures` |
| `generate_villager_trade_gui.py --check` | `checkVillagerTradeGuiTextures` |
| `validate_project_resources.py` | `checkProjectResources` |
| `release/verify_release_contracts.py` | `checkReleaseContracts` |
| `release/record_release_evidence.py` | `recordReleaseEvidence` |

## Generacion manual

- `generate_machine_gui.py` y `generate_villager_trade_gui.py`: regeneran sus
  texturas sin `--check`.
- `generate_enchantment_levels.py`: mantiene numerales romanos 11-255 en idiomas.

Los generadores antiguos de capturadores se retiraron porque no reproducian los
PNG actuales. No recrearlos sin una fuente visual canonica nueva.

## Rendimiento

Lee primero `performance/README.md`. Entradas principales:

- `run_server_benchmark.py` y `run_client_benchmark.py`: mediciones reales.
- `compare_results.py` y `compare_client_results.py`: aceptacion comparable.
- `prepare_template_manifest.py`, `prepare_machine_matrix.py` e
  `inspect_world_block_entities.py`: plantillas reales selladas.
- `run_java_benchmark.py`: equivalencia/rendimiento puro con tres escenarios.
- `summarize_jfr.py`: vistas de una grabacion JFR.
- `platform_tools.py` y `template_contract.py`: soporte interno, no comandos.

Resultados y decisiones historicas versionadas: `performance/RESULTS.md`,
`performance/results/` y `performance/baselines/`.

## Dependencias

Pillow, fijado en `requirements.txt`, solo para imagenes. JFR y microbenchmarks
usan el JDK 25 mediante `JAVA_HOME`, `PATH` o toolchains de Gradle. Ninguna
herramienta necesita PowerShell, Bash, CMD ni coreutils.
