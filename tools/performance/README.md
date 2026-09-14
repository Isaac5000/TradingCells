# Medicion de rendimiento

Estas herramientas son exclusivamente de desarrollo y no entran en el JAR publicado.
Cada resultado conserva mundo, configuracion, commit, JVM, backend y duracion para
poder repetir la comparacion.

Para comprobaciones visuales, `run_client_benchmark.py` admite `--ui-fixture pipe`,
`--ui-fixture rules` o `--ui-fixture crafting` junto con `--open-block`, una plantilla y un mundo
quick-play. Instala el escenario solo en la copia desechable, prueba guardado y
reapertura del editor o recogida/deposito/fabricacion desde la pantalla y captura
el resultado. `--language es_es` permite comprobar traducciones. Estas ejecuciones
quedan marcadas en los metadatos y no son referencias de rendimiento comparables.
`rules` tambien prueba NBT parcial, inversion, copia fantasma de coordenadas sin
consumir el marcador, persistencia y autocompletado real de subcanales desde el servidor.
`interactions` reproduce arrastre vanilla de una pila de 64 mejoras, clics sin
cambio, retirada seguida de otro clic y Shift; comprueba que letras/atajos se
escriben en los campos principales y de reglas sin cerrar ni manipular inventarios.
`caps` situa una conexion de extraccion junto al Almacen de XP transparente para
comprobar su tapa desde una camara interior. Ambas fixtures usan copias desechables.
`materials` captura dos fotogramas del inventario; `connections` instala en la
copia desechable cinco mallas con codos, T, cruces y brazos verticales y captura
la escena desde `--camera`, sin abrir una pantalla. Tambien conserva una segunda
captura para comprobar la animacion. Nunca modifica el mundo original.
`materials` incluye las tres familias completas de mejoras y ambos terminales.
`terminals` coloca los dos terminales orientados hacia el sur para comprobar sus
paneles superiores, laterales y carcasa opaca desde una camara oblicua, sin abrir un menu.
`simulation` comprueba el menu de la granja general y muestra sus diez mejoras
y varios modulos con criaturas. `essences` comprueba la mesa de sintesis.
`simulation-models` coloca seis granjas con creeper, vaca, warden, ghast, bacalao
y dragon, ademas de sus modulos en la barra; comprueba que hay renderizadores y
que un tipo inexistente conserva la base vacia. Espera la sincronizacion del
inventario antes de validar los datos y no abre un menu.
`crafting` comprueba tambien Shift doble clic con un item en el cursor, dos
pilas iguales y una variante con nombre que debe permanecer en el inventario.

Requieren Python 3.11 o posterior y un JDK 25 accesible mediante `JAVA_HOME` o
`PATH`. En los ejemplos, `python` representa ese interprete; en sistemas donde
corresponda puede llamarse `python3`. Las rutas usan la sintaxis portable que
aceptan los tres sistemas.

## Pruebas puras

Insertador de inventario:

```text
python tools/performance/run_java_benchmark.py output-inserter
```

Tooltips de encantamientos por encima del limite vanilla:

```text
python tools/performance/run_java_benchmark.py high-level-tooltip
```

Hipotesis de cache del Autotrader:

```text
python tools/performance/run_java_benchmark.py autotrader-readiness
```

La ultima prueba conserva un umbral estructural del 10 %. Actualmente lo incumple
y documenta una optimizacion descartada; no hay una cache equivalente en produccion.

## Servidor

```text
python tools/performance/run_server_benchmark.py --scenario idle-machines --runs 3 --warmup-seconds 15 --measure-seconds 30
```

Las cargas simples viven en archivos `.txt`. Los escenarios activos, bloqueados,
automatizados, de granjas y de 2.304 intercambios requieren una plantilla ya preparada:

```text
python tools/performance/prepare_template_manifest.py <plantilla> --category server --scenario active-machines --notes "256 maquinas activas con entradas y salidas preparadas"
python tools/performance/run_server_benchmark.py --scenario active-machines --template-directory <plantilla> --setup-command "forceload add -256 -256 -160 -160" --runs 3 --warmup-seconds 15 --measure-seconds 30
```

`--setup-command` se ejecuta por RCON despues de alcanzar `Done` y antes del
calentamiento. Puede repetirse para cargar chunks, fijar reglas u otra
preparacion reproducible; una respuesta de error de Minecraft cancela la
medicion en vez de producir resultados incompletos.

Cada ejecucion produce JFR, perfil de ticks, registro, `runs.csv`,
`summary.csv`, `metadata.txt` y `metadata.json`.
El manifiesto fija la version, el escenario y la carga esperada. La huella SHA-256
del directorio completo impide comparar por accidente dos mundos diferentes.

Resumen textual adicional de cualquier grabacion:

```text
python tools/performance/summarize_jfr.py <grabacion.jfr>
```

Comparacion:

```text
python tools/performance/compare_results.py <baseline> <candidate> --risk structural
```

Los cambios locales usan `--risk local` y un umbral predeterminado del 3 %.
Los estructurales exigen un 10 %. Una regresion primaria superior al 1 % rechaza
el cambio.

## Cliente

Ejemplo reproducible con mundo y camara fijos:

```text
python tools/performance/prepare_template_manifest.py <plantilla> --category client --scenario visible-machines --notes "64 maquinas visibles desde la camara indicada"
python tools/performance/run_client_benchmark.py --backend vulkan --scenario visible-machines --runs 3 --warmup-seconds 15 --measure-seconds 30 --width 1920 --height 1080 --template-directory <plantilla> --quick-play-world world --camera 3.5 82 -6 0 30 --without-rei
```

Opciones relevantes:

- `--backend opengl|vulkan`: exige que el registro confirme el backend efectivo.
- `--without-rei`: retira REI de esa ejecucion.
- `--without-trading-cells`: crea un control vanilla con el mismo grabador.
- `--camera X Y Z YAW PITCH`: fija posicion y orientacion cada fotograma.
- `--open-block X Y Z`: pulsa una vez el bloque cargado y exige que quede abierto
  el menu del Controlador durante toda la medicion.
- `--template-directory`: clona el mismo mundo antes de cada repeticion.
- `--graphics-adapter TEXTO`: registra manualmente la GPU; puede repetirse y evita
  depender de una utilidad concreta del sistema operativo.

Todos los escenarios de cliente exigen una plantilla con manifiesto. La posicion
de camara, resolucion, backend y presencia de REI tambien quedan registradas en
los metadatos. Baseline y candidato deben compartir la misma huella de plantilla.

Una matriz de estres puede prepararse desde maquinas configuradas de un mundo
real sin modificar el original. `--source` puede repetirse para alternar varios
tipos de maquina dentro de la matriz:

```text
python tools/performance/prepare_machine_matrix.py run/vulkan/saves/Test build/performance/templates/villager-machines --source -213 -59 -198 --source -213 -59 -197 --source -213 -59 -199 --source -213 -59 -200 --source -223 -59 -202 --source -213 -59 -202
```

Para crear matrices de las nuevas variantes a partir de una granja configurable
real, se puede repetir una fuente y asociar una sustitucion a cada copia. Se
conservan el aldeano y la espada de la fuente, mientras el bloque obtiene su
familia y objetivo predeterminados de forma normal:

```text
python tools/performance/prepare_machine_matrix.py run/vulkan/saves/Test build/performance/templates/livestock-fish-405 --source -223 -59 -202 --target-block trading_cells:livestock_farm --source -223 -59 -202 --target-block trading_cells:fish_farm
```

El preparador arranca el servidor dedicado, clona los `BlockEntity` mediante
comandos de Minecraft y guarda un manifiesto de cliente. Inventarios, entidades,
herramientas y configuraciones proceden por tanto de maquinas reales.

Para matrices alejadas de los chunks de aparicion, el preparador fuerza primero
el area que contiene fuentes, staging y destino. El ejecutor de servidor tambien
debe recibir un `--setup-command "forceload add ..."`; respuestas como
`That position is not loaded` o `No blocks were cloned` invalidan la ejecucion.

Las dimensiones solicitadas se aplican tambien a la ventana efectiva y cada
pasada se rechaza si Minecraft informa otro tamaño. Esto evita mezclar una primera
ventana Vulkan maximizada con las repeticiones a resolucion fija.

El JFR empieza despues del calentamiento y termina antes de escribir resultados.
Los CSV incluyen tiempo de fotograma medio/p50/p95/p99, FPS, CPU, asignaciones,
memoria residente y trafico. La captura se realiza en un fotograma posterior para
que Vulkan complete la lectura del framebuffer.

Comparacion de rendimiento y capturas:

```text
python tools/performance/compare_client_results.py <baseline> <candidate>
```

Para el control con el mod presente/ausente:

```text
python tools/performance/compare_client_results.py <sin-mod> <con-mod> --stability-only --allow-mod-presence-difference
```

Por defecto las capturas del mismo backend deben ser identicas. Se pueden declarar
tolerancias explicitas con `--maximum-changed-pixels` y
`--maximum-channel-delta`; no hay una tolerancia oculta.

Las plantillas de logistica usan copias del mundo, sin modificar el original:

```text
python tools/performance/prepare_system_matrix.py run/vulkan/saves/Test build/performance/templates/logistics-1024-active --system logistics-1024-active
python tools/performance/prepare_system_matrix.py run/vulkan/saves/Test build/performance/templates/logistics-4096-idle --system logistics-4096-idle
python tools/performance/run_server_benchmark.py --scenario logistics-1024-active --template-directory build/performance/templates/logistics-1024-active --runs 3 --warmup-seconds 15 --measure-seconds 30
```

Hay cuatro escenarios: 1.024/4.096 tuberias y estado active/idle. La matriz
contiene tuberias universales, Almacen lleno, Infusor y terminal. Solo la
plantilla de medida fija sus chunks cargados; el mod no crea tickets.
Para cliente genera otra copia con `--category client`, conservando la huella
de esa copia durante todas las pasadas de ambos backends. `--open-block` abre
un menu de bloque real, sin depender de una clase concreta.
En servidor integrado el grabador posiciona al jugador y abre su MenuProvider
en el hilo del servidor. Es una preparacion de medida, no una prueba del clic
derecho ni de permisos; estos requieren sus propias pruebas funcionales.
El generador imprime su posicion. No ejecutes cargas de servidor y cliente
simultaneamente al medir.

## Matriz

`scenario-matrix.json` registra 1.024 maquinas inactivas, 256 activas, 256
bloqueadas, 256 granjas de las 21 familias en sus estados relevantes,
tolvas/tuberias, 2.304 intercambios, redes de 1.024 y 4.096 tuberias en reposo o
activas, 64 maquinas visibles, control vanilla y las cuatro combinaciones
OpenGL/Vulkan con/sin REI.

`visible-machines.txt` y `vanilla-control.txt` documentan escenas fijas. Las
plantillas con entidades, inventarios, descuentos o tuberias deben conservarse
fuera de Git y reutilizarse en baseline/candidato. Si una plantilla cambia, se
genera de nuevo su manifiesto y se inicia una pareja de mediciones nueva.
