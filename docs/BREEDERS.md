# Criaderos

## Descendencia de aldeanos

Los dos capturadores adultos son entradas persistentes, no plantillas completas
del descendiente. Duplicarlos en creativo no debe duplicar sus datos en la cria.

Cada cria contiene solamente el tipo de entidad, edad inicial -24000 y
VillagerData: variante seleccionada en el criadero, profesion minecraft:none,
nivel 1. No hereda UUID, nombre, ofertas, XP, inventario, equipo, recuerdos,
reputacion ni datos adicionales del progenitor. Al reconstruirse recibe una
identidad nueva. Los capturadores originales permanecen intactos.

Los BabyTemplate pendientes de versiones anteriores se saneaban solo de forma
parcial. Ahora pasan por la misma construccion minima al visualizarse o salir,
tambien despues de guardar y recolocar la maquina. No se pierden crias pendientes.

La correccion del 2026-10-11 no cambia comida, tiempos, variantes seleccionables,
automatizacion, NBT de la maquina ni comportamiento del criadero de piglins.
GameTest: breeder_duplicate_parents_fresh_offspring, incluyendo salida automatica
y plantilla antigua persistida.
