# Hoja de ruta posterior a 1.0.0

Estas ideas quedan fuera de la estabilizacion. Cada una necesita diseño, pruebas
de equivalencia y balance antes de modificar la version estable.

1. **1.1.x - Cultivos mediante datapacks.** Descriptores versionados para entrada,
   produccion, Fortuna, soporte, entorno y etapas visuales. Cada archivo fallara de
   forma aislada y el catalogo integrado seguira disponible como fallback.
2. **1.2.0 - Controlador de Maquinas.** Diagnostico de maquinas detenidas, salidas llenas,
   progreso y XP sin alterar sus estados.
3. **1.2.0 - Distribuidor de XP.** Valvulas y prioridades entre Almacenes, Infusores,
   Traders y granjas mediante la API de fluidos.
4. **1.3.0 - Granja de Artropodos.** Aranas, aranas de cueva, silverfish y
   endermites, con Perdicion de los Artropodos como progresion de herramienta.
5. **Actualizaciones posteriores.** Granjas independientes de blaze, cubos de
   magma y ghasts, seguidas de enderman y shulker, con costes y reglas propias.
6. **Configurador de maquinas.** Copiar filtros y ajustes entre maquinas sin
   copiar inventario, entidades ni XP.
7. **Control por redstone.** Pausa, extraccion de XP y senal de salida llena,
   manteniendo el comportamiento actual cuando no exista senal.

La API de objetivos por datapack y el nucleo puro compartido forman parte de la
1.0.0. Las futuras familias reutilizaran esas reglas por composicion; cada
bloque, tabla y regla de botin seguira perteneciendo a su propia feature.
