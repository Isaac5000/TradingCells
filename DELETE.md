# Rutas prescindibles

Estas rutas no contienen fuentes ni recursos publicados y se pueden regenerar. No se han eliminado automaticamente.

## Eliminacion segura

- `.gradle/`: cache local de Gradle.
- `build/`: clases, recursos procesados, informes y JAR generados.
- `logs/`: registro vacio/residual de ejecuciones locales.
- `run/.cache/`: cache del cliente de desarrollo.
- `run/logs/`: registros del cliente de desarrollo.
- `run/crash-reports/`: informes de cierres ya revisados.
- `run-server/.cache/`: cache del servidor de desarrollo.
- `run-server/logs/`: registros del servidor de desarrollo.

## Conservar salvo decision manual

- `run/saves/`, `run/world/` y `run-server/world/`: contienen mundos de prueba y datos utiles para regresiones.
- `run/config/`, `run-server/config/`, `run/mods/` y `run-server/mods/`: forman el entorno de compatibilidad local.
- `.idea/`: configuracion local de IntelliJ; esta ignorada, pero puede ser util para desarrollar.
- `tools/performance/`: herramientas y resultados reproducibles, no residuos de compilacion.

No se ha encontrado codigo residual de Milk/Cookie ni otra feature versionada que pueda marcarse con seguridad como sobrante.
