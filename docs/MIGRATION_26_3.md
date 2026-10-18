# Migracion a Minecraft 26.3

Versiones: Minecraft 26.3.0, NeoForge 26.3.0.88, Java 25, Gradle 9.8.0.
REI 26.3.824 beta y Jade 26.3.5 siguen siendo opcionales.

## Adaptaciones

- APIs de bloques, renderizado, inventario y configuracion de NeoForge 26.3.
- Teclado y botones fisicos del raton mediante InputConstants/SDL3. Los IDs
  logicos de acciones de contenedor y payloads conservan su significado.
- Alimentos de aldeanos obtenidos del componente VILLAGER_FOOD; tags de mineral
  mediante Tags.BlockItems. El crepitante reconoce hachas por ItemTags.AXES.
- Tablas de botin y modificadores usan condition/modifier y discriminador type.
  La vista previa admite tablas enlazadas o embebidas; los filtros leen las
  nuevas listas de tablas e items/tags. No se sortean drops para calcularla.
- Generadores de JSON actualizados para conservar el formato nuevo.
- Configuracion LOCAL con el nombre anterior trading_cells-common.toml.

## Comercio abierto

La celda ya promocionaba al aldeano y enviaba las ofertas durante la compra.
Se conserva esa ruta: recalcula precios, persiste el aldeano y envia un snapshot
con revision, nivel, XP, ofertas y seleccion al mismo menu abierto.
El GameTest villager_level_up_open_menu compra al cruzar el umbral de nivel y
comprueba tanto el estado del menu como el payload enviado al cliente.

## Evidencia

- OpenGL con REI: artifacts/migration-26.3-opengl-storm y
  artifacts/migration-26.3-opengl-machines. Armas, recargas, 18 menus,
  controles de cantera/granja, 12 categorias REI y granja de interacciones.
- Vulkan sin REI: artifacts/migration-26.3-vulkan-machines-20261018.
  Los 18 menus, controles y modelos de interacciones completan la fixture;
  summary.csv confirma el backend Vulkan a 1280x720, escala automatica.
- Servidor: check, checkLogisticsResources, checkMobSimulationResources y
  runGameTestServer correctos; 170 GameTests obligatorios, incluido comercio
  con promocion y envio de ofertas al menu abierto. Registro completo:
  artifacts/migration-26.3-validation-20261018-2.log.

Las pruebas visuales usan copias desechables de un mundo 26.2. Sus mensajes de
recetas antiguas retiradas proceden de ese mundo. REI beta emite avisos upstream
sobre OnlyIn; el arranque y las categorias se comprueban igualmente.
No se ha ejecutado una validacion de publicacion clean releaseCheck.
