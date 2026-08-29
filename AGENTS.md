# Guia breve para agentes

1. Lee `PROJECT_CONTEXT.md` para conocer invariantes, decisiones descartadas y estado.
2. Usa `PROJECT_INDEX.md` solo como enrutador y abre la fila de la feature afectada.
3. Lee la documentacion enlazada solo si el cambio toca ese contrato.

Busca con `rg` dentro de rutas concretas. No indexes `.gradle/`, `build/`, `logs/`,
`run/` ni `run-server/`; son caches, resultados o mundos locales. Evita releer
JSON de idiomas, catalogos completos y resultados historicos salvo que la tarea
los afecte.

Antes de editar, revisa `git status` y conserva cambios ajenos. Mantiene IDs, NBT,
payloads y mecanicas salvo peticion explicita. Valida primero la tarea dirigida
del indice y termina con `check`; usa `releaseCheck` para cambios publicables.
