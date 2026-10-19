# Granjas de entidades: alcance actual

Estado de desarrollo al 2026-10-18, no una publicacion. `mob_farm` simula muerte
y botin; `entity_interaction_farm` obtiene recursos por interacciones independientes.
El objetivo se guarda en un modelo de criatura. `MobFarmCatalog` conserva
agrupaciones y metadatos ampliables. No existen bloques separados para
esqueletos, zombis, saqueadores, creepers, artropodos, criaturas acuaticas u
otras familias.

## Contrato actual

- El modelo mantiene ID, estado saneado y clasificacion de la entidad. No hace
  falta registrar una granja nueva para cada especie.
- El botin se ejecuta desde la tabla nativa de la criatura reconstruida; el
  registro de entidades y los datos guardados permiten compatibilidad con mods.
  Un descriptor `mob_farm_target` de esquema 1 amplia el catalogo, no es un
  permiso universal obligatorio para reconstruir cualquier entidad.
- La granja de interacciones mantiene dos canales, objeto y herramienta,
  aislamiento y resultados pendientes atomicos. No genera XP ni mata al objetivo.
- Velocidad y capacidad son mejoras compartidas. Los bloques de esencias y
  generacion de huevos tienen sus recetas, registros y contratos propios.

## Limites

No crear nuevos bloques por familia ni recuperar los retirados. Mantener IDs,
persistencia, REI/Jade opcionales y pruebas. Las entidades ausentes no deben
perder sus datos; la compatibilidad real depende de sus APIs, serializacion y
botin, no solo de aparecer en un catalogo.

No aplicar a las esencias la blacklist de la mesa de huevos: Wither y Dragon
estan bloqueados por defecto para fabricar huevos, no excluidos globalmente de
la simulacion. La granja de interacciones rechaza capturas, operaciones diferidas
y efectos que salgan del entorno; extensiones requieren handlers compatibles.

Contratos: `ESSENCE_AUTOMATION.md`, `ENTITY_INTERACTION_FARM.md` y
`MOB_FARM_DATAPACKS.md`. El historial visual permanece en `ENTITY_SIMULATION_REWORK.md`.
