# Medición de rendimiento

Estas herramientas no forman parte del JAR publicado. Preparan un mundo aislado, lo
copian antes de cada ejecución y usan tres ejecuciones calentadas para calcular la
mediana.

## Equivalencia del insertador

```powershell
.\tools\performance\verify_output_inserter.ps1
```

Compara 250.000 inventarios aleatorios contra el algoritmo anterior. La prueba falla
si cambia la capacidad calculada, el contenido final o el orden de llenado de slots.

## Servidor

```powershell
python .\tools\performance\run_server_benchmark.py `
  --scenario idle-machines --runs 3 --warmup-seconds 15 --measure-seconds 30
```

`idle-machines.txt` coloca 1.024 máquinas inactivas. `idle-control.txt` coloca el
mismo volumen de bloques sin ticker y sirve para estimar el límite máximo de una
suspensión completa. Se puede pasar cualquier carga con `--workload`.

Cada resultado contiene `runs.csv`, `summary.csv`, logs, perfiles de `/debug` y una
grabación JFR de Minecraft. Para extraer CPU y asignaciones:

Al comparar dos versiones, pasa la plantilla de la primera medición mediante
`--template-directory <baseline>\template`; cada ejecución recibirá una copia exacta
de ese mundo y solo se sustituirán las propiedades locales del servidor.

```powershell
.\tools\performance\summarize_jfr.ps1 -Recording <archivo.jfr>
```

Para comparar una medición anterior y otra candidata:

```powershell
python .\tools\performance\compare_results.py <baseline> <candidate>
```

El comparador exige una mejora mínima del 10 % en MSPT medio o p95 y rechaza una
regresión superior al 2 % en esas latencias. CPU, asignaciones, paquetes y escrituras
se muestran como métricas de diagnóstico; la CPU se compara en puntos porcentuales
porque las muestras de servidores inactivos son muy próximas a cero. Los escenarios activos, bloqueados, intercambios masivos
y cliente deben usar copias de cargas equivalentes y conservar el mismo mundo,
semilla, configuración, calentamiento y duración.
