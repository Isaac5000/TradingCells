# Medicion de rendimiento

Estas herramientas son exclusivamente de desarrollo y no entran en el JAR publicado.
Cada resultado conserva mundo, configuracion, commit, JVM, backend y duracion para
poder repetir la comparacion.

## Pruebas puras

Insertador de inventario:

```powershell
.\tools\performance\verify_output_inserter.ps1
```

Tooltips de encantamientos por encima del limite vanilla:

```powershell
.\tools\performance\verify_high_level_tooltip.ps1
```

Hipotesis de cache del Autotrader:

```powershell
.\tools\performance\verify_autotrader_readiness.ps1
```

La ultima prueba conserva un umbral estructural del 10 %. Actualmente lo incumple
y documenta una optimizacion descartada; no hay una cache equivalente en produccion.

## Servidor

```powershell
python .\tools\performance\run_server_benchmark.py `
  --scenario idle-machines --runs 3 --warmup-seconds 15 --measure-seconds 30
```

Las cargas simples viven en archivos `.txt`. Los escenarios activos, bloqueados,
automatizados y de 2.304 intercambios requieren una plantilla ya preparada:

```powershell
python .\tools\performance\run_server_benchmark.py `
  --scenario active-machines `
  --template-directory <plantilla> `
  --runs 3 --warmup-seconds 15 --measure-seconds 30
```

Cada ejecucion produce JFR, perfil de ticks, registro, `runs.csv`,
`summary.csv`, `metadata.txt` y `metadata.json`.

Comparacion:

```powershell
python .\tools\performance\compare_results.py <baseline> <candidate> --risk structural
```

Los cambios locales usan `--risk local` y un umbral predeterminado del 3 %.
Los estructurales exigen un 10 %. Una regresion primaria superior al 1 % rechaza
el cambio.

## Cliente

Ejemplo reproducible con mundo y camara fijos:

```powershell
python .\tools\performance\run_client_benchmark.py `
  --backend vulkan --scenario visible-machines --runs 3 `
  --warmup-seconds 15 --measure-seconds 30 `
  --width 1920 --height 1080 `
  --template-directory <plantilla> --quick-play-world world `
  --camera 3.5 82 -6 0 30 --without-rei
```

Opciones relevantes:

- `--backend opengl|vulkan`: exige que el registro confirme el backend efectivo.
- `--without-rei`: retira REI de esa ejecucion.
- `--without-trading-cells`: crea un control vanilla con el mismo grabador.
- `--camera X Y Z YAW PITCH`: fija posicion y orientacion cada fotograma.
- `--template-directory`: clona el mismo mundo antes de cada repeticion.

El JFR empieza despues del calentamiento y termina antes de escribir resultados.
Los CSV incluyen tiempo de fotograma medio/p50/p95/p99, FPS, CPU, asignaciones,
memoria residente y trafico. La captura se realiza en un fotograma posterior para
que Vulkan complete la lectura del framebuffer.

Comparacion de rendimiento y capturas:

```powershell
python .\tools\performance\compare_client_results.py <baseline> <candidate>
```

Para el control con el mod presente/ausente:

```powershell
python .\tools\performance\compare_client_results.py <sin-mod> <con-mod> `
  --stability-only --allow-mod-presence-difference
```

Por defecto las capturas del mismo backend deben ser identicas. Se pueden declarar
tolerancias explicitas con `--maximum-changed-pixels` y
`--maximum-channel-delta`; no hay una tolerancia oculta.

## Matriz

`scenario-matrix.json` registra 1.024 maquinas inactivas, 256 activas, 256
bloqueadas, tolvas/tuberias, 2.304 intercambios, 64 maquinas visibles, control
vanilla y las cuatro combinaciones OpenGL/Vulkan con/sin REI.

`visible-machines.txt` y `vanilla-control.txt` generan escenas fijas. Las
plantillas con entidades, inventarios, descuentos o tuberias deben conservarse
fuera de Git y reutilizarse en baseline/candidato.
