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
4. **Granjas futuras.** Animales de granja y peces son los siguientes grupos
   prioritarios. El inventario completo, candidatos y exclusiones vive en
   [`MOB_FARM_ROADMAP.md`](MOB_FARM_ROADMAP.md).
5. **Configurador de maquinas.** Copiar filtros y ajustes entre maquinas sin
   copiar inventario, entidades ni XP.
6. **Control por redstone.** Pausa, extraccion de XP y senal de salida llena,
   manteniendo el comportamiento actual cuando no exista senal.

La API de objetivos por datapack y el nucleo compartido de las diez familias
configurables forman parte de la 1.0.0. Las futuras familias reutilizaran esas
reglas por composicion; cada bloque y sus recursos seguiran teniendo IDs propios.
