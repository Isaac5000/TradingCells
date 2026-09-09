# Estado de hitos previo a publicar 1.0.0

La version sigue sin publicarse. Los sistemas aprobados se incorporaron al arbol
de desarrollo y la matriz final se mantiene aplazada hasta congelar funciones.

1. **Completado - Cultivos mediante datapacks.** El contrato
   `schema_version: 1` cubre entrada, produccion, Fortuna, soporte, entorno y
   etapas visuales mediante snapshots inmutables y fallback integrado.
2. **Retirado - Controlador y red experimental de XP.** Eliminados por decision
   expresa; sin migracion porque el mod no se ha publicado.
3. **En curso - Red logistica universal.** Cinco tuberias, llave, capacidades
   estandar, gases mediante fluidos y terminales. Pendientes y evidencia actual:
   [`LOGISTICS.md`](LOGISTICS.md).
4. **Completado - Granjas principales.** Acuaticas, Anfibios, Monturas, Abejas y
   Creakings tienen familias propias; los Vexes se integran en Saqueadores. El
   inventario completo, candidatos y exclusiones vive en
   [`MOB_FARM_ROADMAP.md`](MOB_FARM_ROADMAP.md).
5. **Retirado - Configurador de maquinas.** Eliminado por peticion expresa.
   Las tuberias usan su llave, perfiles en mejoras y selector de destino propios.
6. **Completado - Control por redstone.** Modos ignorar, señal alta pausa y señal
   baja pausa, con salida de comparador para inventarios llenos. No extrae XP por
   redstone.
7. **Pendiente futuro.** Sniffers, Striders y fauna salvaje permanecen aplazados;
   Hoglins, jefes y demas exclusiones siguen fuera por decision expresa.
8. **Pendiente de cierre.** Congelar funciones y ejecutar la matriz manual,
   compatibilidad externa, mundo existente, instalacion limpia y dos JAR finales
   reproducibles.

Las API de objetivos y cultivos por datapack y el nucleo compartido de las
diecisiete familias configurables forman parte del arbol de desarrollo de la
1.0.0. Las
futuras familias reutilizaran esas reglas por composicion; cada bloque y sus
recursos seguiran teniendo IDs propios.
