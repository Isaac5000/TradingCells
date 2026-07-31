# Informe de pruebas

Fecha: 2026-07-27

## Verificaciones ejecutadas

### Compilación y tests

Comando:

```powershell
.\gradlew.bat clean build verifyDomainRules checkArchitecture
```

Resultado: **BUILD SUCCESSFUL**.

Tareas relevantes completadas:

- `compileJava`
- `processResources`
- `jar`
- `compileTestJava`
- `test`
- `verifyDomainRules`
- `checkArchitecture`
- `build`

### Texturas de máquinas

Comando:

```powershell
python tools\generate_machine_gui.py --check
```

Resultado: **Machine GUI textures valid**.

Comprueba dimensiones, presencia y reglas de los sprites de slots.

### GUI de Trader

Comando:

```powershell
python tools\generate_villager_trade_gui.py --check
```

Resultado: **Validated neutral villager GUI texture and widgets at 348x210**.

Comprueba tamaños, RGBA de filas, variantes requeridas y simetría de las
flechas.

### Idiomas

Los dos JSON se cargaron con parser estructurado. Resultado:

- `en_us.json`: válido.
- `es_es.json`: válido.
- Paridad exacta: 100 claves en ambos.

### Higiene del diff

`git diff --check` no detectó errores de whitespace. Solo informó de la
conversión esperada LF/CRLF en el entorno Windows.

## Reglas de dominio verificadas

`DomainRulesVerification` cubre:

- ciclo de vida y selección de ofertas;
- expiración independiente de descuentos por oferta;
- renovación de una sola capa sin acumulación de magnitud;
- identidad estable aunque cambie el orden de ofertas;
- programación de la expiración temporal más próxima;
- procesos pausados, completados y reiniciados;
- los cinco costes exactos del criadero de piglins;
- escalado de progreso de la granja;
- transiciones de infección y curación del convertidor;
- multiplicadores y ventana visual de la granja de hierro;
- reglas de trueque de piglins y de leche/galleta;
- centrado de entidades capturadas;
- geometría compartida de Trader y Autotrader.

## Comprobación visual estática

Se inspeccionaron los PNG regenerados:

- placeholder de poción;
- placeholder de manzana;
- fila de intercambio;
- flecha hover.

Los recursos son visibles, tienen transparencia correcta y coinciden con las
dimensiones validadas por script.

## Pruebas que requieren revisión manual en Minecraft

No se inició un cliente ni un servidor de Minecraft durante esta entrega. Por
ello no se presentan como ejecutadas las siguientes pruebas:

- escalas GUI pequeña, normal, grande y automática;
- todos los idiomas y resoluciones dentro del cliente;
- hover, click, drag, teclas numéricas y tooltips en una sesión real;
- inserción/extracción por tolva o capability;
- descarga de chunk, reinicio de mundo y movimiento de bloque portátil;
- sincronización con dos jugadores;
- ciclo completo con cada alimento del criadero;
- combinación real de variantes de poción y rechazo de la unidad 65;
- expiración y renovación del descuento durante una sesión prolongada;
- restock, ofertas agotadas y curación antes/durante el descuento;
- medición con profiler del lote de 64x36 intercambios.

Estas pruebas forman la matriz manual recomendada tras instalar el JAR generado
por `build`. La ausencia de una sesión runtime es una limitación de verificación,
no un fallo conocido de compilación.

## Observaciones

- El inventario del jugador conserva la regla vanilla de stack máximo 1 para
  pociones. La máquina puede almacenar 64 internamente y entrega unidades
  separadas al exterior.
- Las cuatro alertas históricas sobre restas de `long` carecen de ubicación en
  el árbol actual; están marcadas en `SONAR_REVIEW.md` para una ejecución fresca
  de Sonar.

