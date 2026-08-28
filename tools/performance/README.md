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
automatizados, de granjas y de 2.304 intercambios requieren una plantilla ya preparada:

```powershell
python .\tools\performance\prepare_template_manifest.py <plantilla> `
  --category server --scenario active-machines `
  --notes "256 maquinas activas con entradas y salidas preparadas"

python .\tools\performance\run_server_benchmark.py `
  --scenario active-machines `
  --template-directory <plantilla> `
  --setup-command "forceload add -256 -256 -160 -160" `
  --runs 3 --warmup-seconds 15 --measure-seconds 30
```

`--setup-command` se ejecuta por RCON despues de alcanzar `Done` y antes del
calentamiento. Puede repetirse para cargar chunks, fijar reglas u otra
preparacion reproducible; una respuesta de error de Minecraft cancela la
medicion en vez de producir resultados incompletos.

Cada ejecucion produce JFR, perfil de ticks, registro, `runs.csv`,
`summary.csv`, `metadata.txt` y `metadata.json`.
El manifiesto fija la version, el escenario y la carga esperada. La huella SHA-256
del directorio completo impide comparar por accidente dos mundos diferentes.

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
python .\tools\performance\prepare_template_manifest.py <plantilla> `
  --category client --scenario visible-machines `
  --notes "64 maquinas visibles desde la camara indicada"

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

Todos los escenarios de cliente exigen una plantilla con manifiesto. La posicion
de camara, resolucion, backend y presencia de REI tambien quedan registradas en
los metadatos. Baseline y candidato deben compartir la misma huella de plantilla.

Una matriz de estres puede prepararse desde maquinas configuradas de un mundo
real sin modificar el original. `--source` puede repetirse para alternar varios
tipos de maquina dentro de la matriz:

```powershell
python .\tools\performance\prepare_machine_matrix.py `
  .\run\vulkan\saves\Test `
  .\build\performance\templates\villager-machines `
  --source -213 -59 -198 --source -213 -59 -197 `
  --source -213 -59 -199 --source -213 -59 -200 `
  --source -223 -59 -202 --source -213 -59 -202
```

El preparador arranca el servidor dedicado, clona los `BlockEntity` mediante
comandos de Minecraft y guarda un manifiesto de cliente. Inventarios, entidades,
herramientas y configuraciones proceden por tanto de maquinas reales.

Para matrices alejadas de los chunks de aparicion, el preparador fuerza primero
el area que contiene fuentes, staging y destino. El ejecutor de servidor tambien
debe recibir un `--setup-command "forceload add ..."`; respuestas como
`That position is not loaded` o `No blocks were cloned` invalidan la ejecucion.

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
bloqueadas, 256 Granjas de Esqueletos/Zombis en sus estados relevantes,
tolvas/tuberias, 2.304 intercambios, 64 maquinas visibles, control vanilla y las
cuatro combinaciones OpenGL/Vulkan con/sin REI.

`visible-machines.txt` y `vanilla-control.txt` documentan escenas fijas. Las
plantillas con entidades, inventarios, descuentos o tuberias deben conservarse
fuera de Git y reutilizarse en baseline/candidato. Si una plantilla cambia, se
genera de nuevo su manifiesto y se inicia una pareja de mediciones nueva.
