# Estado de hitos previo a publicar 1.0.0

Resumen actualizado el 2026-10-18. Version de desarrollo no publicada; entorno
y versiones en `gradle.properties`, rutas y matriz de pruebas en `PROJECT_INDEX.md`.

1. **Completado - Cultivos mediante datapacks.** El contrato
   `schema_version: 1` cubre entrada, produccion, Fortuna, soporte, entorno y
   etapas visuales mediante snapshots inmutables y fallback integrado.
2. **Retirado - Controlador y red experimental de XP.** Eliminados por decision
   expresa; sin migracion porque el mod no se ha publicado.
3. **Implementado - Red logistica universal.** Cinco tuberias, llave, capacidades
   estandar, gases mediante fluidos y terminales. Limites y evidencia actual:
   [`LOGISTICS.md`](LOGISTICS.md).
4. **Implementado - Granjas generales.** `mob_farm` usa modelos de criatura y
   botin real; `entity_interaction_farm` obtiene recursos por interacciones.
   Las familias son datos del catalogo, no bloques separados. Alcance en
   [`MOB_FARM_ROADMAP.md`](MOB_FARM_ROADMAP.md).
5. **Retirado - Configurador de maquinas.** Eliminado por peticion expresa.
   Las tuberias usan su llave, perfiles en mejoras y selector de destino propios.
6. **Implementado - Pausa y control de maquinas.** Conservar las opciones propias
   de cada bloque. La granja de interacciones tiene pausa manual, no redstone.
7. **Implementado - Esencias y equipo.** Cuatro tiers, estabilizador, mesa de
   modelos, mesa de huevos, Sintetizador de materia y equipo electrico con un
   condensador de 256 cargas. Contratos en `ESSENCE_AUTOMATION.md`,
   `ARCANE_INFUSER.md` y `ELECTRIC_EQUIPMENT.md`.
8. **Pendiente de cierre.** Congelar funciones y ejecutar la matriz manual,
   compatibilidad externa, mundo existente, instalacion limpia y dos JAR finales
   reproducibles.

Pendientes tecnicos de rendimiento y limpieza: `AUDIT_20261018.md`, incluida su
seccion de cierre. No restaurar granjas retiradas ni repetir optimizaciones
rechazadas sin nuevas mediciones comparables.
