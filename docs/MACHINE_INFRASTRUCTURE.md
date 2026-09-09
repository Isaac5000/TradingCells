# Infraestructura de maquinas

Este documento resume los contratos del diagnostico comun y la configuracion interna.
El transporte por capacidades se documenta en `LOGISTICS.md`. La implementacion sigue siendo
la fuente de verdad; `PROJECT_INDEX.md` contiene las rutas concretas.

## Diagnostico comun

Las maquinas exponen una instantanea de solo lectura con estado, motivo, progreso,
XP y ocupacion de salida. Los cuatro estados persistentes del contrato son
`RUNNING`, `INACTIVE`, `PAUSED` y `BLOCKED`; cada feature conserva sus propios
motivos. Jade consume la instantanea sin ejecutar ciclos
ni calcular botin en cliente.

## Configuracion interna y redstone

- El objeto Configurador, su receta y su interaccion se han retirado. Los ajustes
  persistentes existentes y los contratos internos se conservan; no hay otra
  herramienta que copie ajustes de maquinas. La copia de caras de tuberia vive
  exclusivamente en su menu.
- Los contratos internos exigen el mismo ID de bloque y un esquema valido para
  aplicar una configuracion de forma atomica.
- Solo se copian objetivos, filtros y modos configurables. Inventarios,
  trabajadores, entidades, ofertas, XP, progreso, botin pendiente, temporizadores
  y estado aleatorio quedan fuera.
- Una pausa conserva el progreso exacto. Un comparador lee 15 cuando ninguna
  salida activa admite mas produccion y 0 en los demas casos.
- Almacenes, comercio manual e Infusores no aceptan pausa de
  redstone.

## Experiencia por capacidades

El Distribuidor, la tuberia de XP y el Controlador experimentales se han retirado.
La API publica de fluidos conserva la equivalencia de un punto de XP por unidad.
No se expone energia o fluido donde una maquina no los use realmente.

Fuentes actuales: las 21 familias de granjas, Trocador y Autotrocador. Destino:
Infusor Arcano. El Almacen de Experiencia es bidireccional.

## Compatibilidad y pruebas

Los IDs, NBT y payloads son aditivos y estan fijados por
`tools/release/contracts-1.0.0.json`. REI y Jade siguen siendo opcionales y las
pantallas no entran en el servidor dedicado. Las suites por feature cubren
persistencia, rechazo atomico, redstone y limites enteros. La evidencia y la
cobertura pendiente de las tuberias estan separadas en `LOGISTICS.md`.
