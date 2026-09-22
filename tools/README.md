# Herramientas

Indice para evitar recorrer `tools/` completo. Todas las herramientas conservadas
son Python 3.11+ portable y resuelven rutas desde `__file__` o argumentos.

## Invocadas por Gradle

| Script | Tarea / responsabilidad |
| --- | --- |
| `check_portability.py` | `checkPortability`; SO, rutas, shells, case y residuos |
| `generate_machine_gui.py --check` | `checkMachineGuiTextures` |
| `generate_villager_trade_gui.py --check` | `checkVillagerTradeGuiTextures` |
| `generate_logistics_resources.py --check` | `checkLogisticsResources`; incluye `verify_pipe_models.py` |
| `test_logistics_resources.py` | `testLogisticsResourceValidation`; regresiones de comparacion PNG, integrada en `checkLogisticsResources` |
| `generate_family_upgrades.py --check` | `checkUpgradeFamilyTextures`; veinticinco mejoras con marcos comunes y dos terminales, integrada en `checkLogisticsResources` |
| `validate_project_resources.py` | `checkProjectResources` |
| `test_recipe_resources.py` | `testRecipeResources`; recetas y registro vanilla, requiere `createMinecraftArtifacts` con fuentes |
| `release/verify_release_contracts.py` | `checkReleaseContracts` |
| `release/record_release_evidence.py` | `recordReleaseEvidence` |

## Antes de subir a GitHub

Ejecutar `./gradlew clean build releaseCheck` (`.\gradlew.bat` en Windows).
La subida de Git y las comprobaciones de GitHub Actions son operaciones distintas:
un commit puede estar subido aunque Actions termine en rojo.

Los cambios intencionados de recetas requieren revisar el catalogo y actualizar
solo `recipe_catalog_count` y `recipe_catalog_sha256` en
`release/contracts-1.0.0.json`. El comprobador muestra los valores actuales si
difieren. No regenerar esa referencia automaticamente ni desactivar el control:
debe seguir detectando cambios accidentales.

NeoForge mantiene `disableRecompilation = false` tambien con `CI=true`, porque
las pruebas de recetas leen `Items.java` del JAR de fuentes de Minecraft. El modo
binario predeterminado de CI no genera ese archivo.

## Generacion manual

- `generate_machine_gui.py` y `generate_villager_trade_gui.py`: regeneran sus
  texturas sin `--check`.
- `generate_enchantment_levels.py`: mantiene numerales romanos 11-255 en idiomas.
- `generate_pipe_textures.py`: valida los sprites editables por tipo de tuberia;
  solo `--write` los sobrescribe. `--preview <directorio>` junto con `--write`
  genera una vista PNG/GIF. Fotogramas de 32x32 con escala/fase comunes.
- `generate_logistics_resources.py`: modelos organizados por tipo, recetas y
  llave de 16x16. No sobrescribe los PNG editables de bloques.
- `generate_family_upgrades.py --write`: compone los emblemas originales sobre
  los cinco marcos comunes de quarry, recoloreados con rampas de luminancia.
  Conserva las fuentes y las cinco mejoras originales. Fuentes y prompts
  en `assets/upgrade_bases/README.md`. `--preview <PNG>` muestra todos los niveles.
- `generate_family_upgrades.py --bake-terminal-steel --write`: guarda el acabado
  gris de los terminales en sus PNG, usando las fuentes de cobre conservadas en
  `assets/upgrade_bases/originals/terminals/`; no cambia sus formas ni pantallas.

Los generadores antiguos de capturadores se retiraron porque no reproducian los
PNG actuales. No recrearlos sin una fuente visual canonica nueva.

La comprobacion de PNG generados compara dimensiones, fotogramas y pixeles RGBA,
no los bytes comprimidos, que pueden variar entre plataformas. Los JSON conservan
la comparacion exacta; las diferencias reales de color y transparencia se rechazan.

## Rendimiento

Lee primero `performance/README.md`. Entradas principales:

- `run_server_benchmark.py` y `run_client_benchmark.py`: mediciones reales.
- `compare_results.py` y `compare_client_results.py`: aceptacion comparable.
- `prepare_template_manifest.py`, `prepare_machine_matrix.py`,
  `prepare_system_matrix.py` e `inspect_world_block_entities.py`: plantillas
  reales selladas, incluidas redes y Controladores de 1.024 bloques.
- `run_java_benchmark.py`: equivalencia/rendimiento puro con tres escenarios.
- `summarize_jfr.py`: vistas de una grabacion JFR.
- `platform_tools.py` y `template_contract.py`: soporte interno, no comandos.

Resultados y decisiones historicas versionadas: `performance/RESULTS.md`,
`performance/results/` y `performance/baselines/`.

## Dependencias

Pillow, fijado en `requirements.txt`, solo para imagenes. JFR y microbenchmarks
usan el JDK 25 mediante `JAVA_HOME`, `PATH` o toolchains de Gradle. Ninguna
herramienta necesita PowerShell, Bash, CMD ni coreutils.
