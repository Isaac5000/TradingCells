# Granja general de entidades

La unica granja de entidades publicada es `mob_farm`. Su objetivo se elige con
esencias y modulos, mientras `MobFarmCatalog` resuelve las entidades vanilla y
las familias definidas por tags dinamicos. No existen bloques separados para
esqueletos, zombis, saqueadores, creepers, artropodos, criaturas acuaticas u
otras familias.

## Contrato actual

- `mob_farm` registra el bloque, menu, mejoras, workbench y estabilizador.
- Las entidades seleccionables se describen mediante tags y descriptores
  `mob_farm_target` de esquema 1.
- El loot se ejecuta desde la tabla nativa de la entidad y admite entidades de
  otros mods cuando su datapack aporta los datos compatibles.
- Las entidades sin botin util completan ciclos de experiencia, pero no crean
  un bloque o receta de granja adicional.

## Limites

No se publican nuevos bloques por familia. Una ampliacion futura debe extender
el catalogo dinamico, sus tags o los descriptores de `mob_farm`, manteniendo el
selector, la persistencia, REI, Jade y los GameTests del bloque general.

Quedan fuera las entidades tecnicas, jefes y progresion especial, aldeanos y
comerciantes, y criaturas sin una mecanica de produccion justificable.
