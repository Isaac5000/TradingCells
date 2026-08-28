# Toque de Seda II

Trading Cells genera `minecraft:silk_touch` a nivel 2 mediante el Infusor
Arcano. No existe un encantamiento paralelo: las comprobaciones estandar de
Minecraft y de otros mods lo detectan como Toque de Seda normal, mientras que
Trading Cells reserva el nivel 2 para los bloques especiales de esta lista.

## Obtencion

```text
Fragmento de eco | Fragmento de amatista | Fragmento de eco
Huevo de tortuga | Libro con Toque de Seda I | Huevo de tortuga
Fragmento de eco | Estrella del Nether | Fragmento de eco
```

Coste: `75.000` XP. La entrada central debe ser un libro que contenga solo Toque
de Seda I; asi no se consumen por accidente libros mejorados o con otros
encantamientos. El resultado se aplica en un yunque a las mismas herramientas
que admiten Toque de Seda vanilla. Dos libros de Toque de Seda I no crean el
nivel II porque el nivel maximo vanilla no se modifica.

## Bloques implementados

| Bloque | Identificador | Herramienta necesaria | Datos conservados |
| --- | --- | --- | --- |
| Generador | `minecraft:spawner` | Pico | Entidad, potenciales, retrasos y configuracion completa |
| Generador de desafio | `minecraft:trial_spawner` | Pico | Entidad, configuracion, estado, criaturas activas, participantes, rearme, control por redstone y variante ominosa |
| Arca | `minecraft:vault` | Pico | Configuracion, estado y recompensas pendientes; nunca UUID permanentes de jugadores |
| Pizarra profunda reforzada | `minecraft:reinforced_deepslate` | Pico | No tiene datos adicionales |
| Brotador de amatista | `minecraft:budding_amethyst` | Pico | No tiene datos adicionales |
| Arena sospechosa | `minecraft:suspicious_sand` | Pala | Contenido arqueologico exacto o tabla y semilla; no progreso de cepillado |
| Grava sospechosa | `minecraft:suspicious_gravel` | Pala | Contenido arqueologico exacto o tabla y semilla; no progreso de cepillado |
| Nieve en polvo | `minecraft:powder_snow` | Pala | No tiene datos adicionales |
| Huevas de rana | `minecraft:frogspawn` | Hacha, pico, pala o azada con Toque de Seda II | No tiene datos adicionales |
| Tarta | `minecraft:cake` | Hacha, pico, pala o azada con Toque de Seda II | Porciones restantes |

Los tags `#trading_cells:silk_touch_two/pickaxe`,
`#trading_cells:silk_touch_two/shovel` y
`#trading_cells:silk_touch_two/general` permiten que datapacks amplien la lista
sin sustituir la logica de botin.

Los bloques tambien se incorporan a los tags vanilla `mineable` de sus
herramientas. La velocidad eficiente solo se conserva cuando la herramienta
correcta lleva Toque de Seda II; con la herramienta correcta pero sin el nivel
II, Trading Cells limita la velocidad para evitar destruir accidentalmente un
bloque especial. Una herramienta de la clase equivocada mantiene la velocidad
vanilla que le corresponda.

## Conservacion y compatibilidad

El resultado se agrega mediante un modificador global de botin. No se reemplazan
las tablas vanilla completas, por lo que otros datapacks y modificadores pueden
seguir componiendo sus resultados. Si otra tabla ya entrega el mismo bloque, no
se agrega una segunda copia.

El generador normal y el generador de desafio son tipos protegidos por Minecraft:
un jugador no operador no puede aplicar libremente su NBT desde cualquier
objeto. Trading Cells marca exclusivamente los bloques obtenidos mediante Toque
de Seda II y restaura sus datos al recolocarlos. La misma ruta conserva tambien
el arca y los bloques sospechosos, que son los otros casos con Block Entity de
esta lista.

La arena y la grava sospechosas conservan su contenido para que recogerlas no
permita volver a sortear el botin. El progreso visual de cepillado se reinicia al
recolocarlas. La tarta conserva exactamente las porciones restantes.

## Generadores conservados

Un generador obtenido legitimamente mediante Toque de Seda II muestra en el
inventario una vista fija de su entidad. Esa vista no gira ni avanza
animaciones. La advertencia roja de NBT protegido se oculta solo en el objeto
marcado por esta mecanica; los objetos NBT creados por comandos siguen mostrando
la proteccion vanilla.

Al usar un generador colocado se extrae su entidad como huevo generador sin
destruir el bloque. El generador queda vacio y no puede volver a producir otro
huevo hasta recibir una entidad nueva. Una entidad sin modificaciones produce
el huevo vanilla normal. Si contiene equipo, efectos, pasajeros, montura u otros
datos, el huevo conserva la jerarquia completa, recibe brillo y muestra un
resumen localizado con los campos relevantes. El huevo confiable tampoco muestra
la advertencia roja y, al usarse, consume una unidad y genera la jerarquia
guardada.

Para elegir el icono y la entidad descrita se recorre la cadena desde el
pasajero superior hacia la montura y se prioriza la primera entidad cuyo modelo
admite armadura humanoide. Si ninguna la admite, se usa el jinete superior. El
tag `#trading_cells:humanoid_armor_models` permite ampliar esta clasificacion
mediante datapacks. Los campos vacios de armadura, efectos, pasajeros o montura
se omiten del tooltip.

La integracion opcional con Jade muestra el requisito de Toque de Seda II y la
entidad configurada al observar el generador colocado.

## Control por redstone

Un clic derecho con un comparador instala permanentemente el control por
redstone en un generador normal o de desafio y consume el comparador. REI muestra
esta interaccion para ambos bloques. La configuracion se conserva al recoger y
recolocar el bloque con Toque de Seda II.

Con una senal constante, el generador normal detiene por completo su operacion y
la entidad visible queda inmovil. En el generador de desafio la senal impide
iniciar una prueba nueva; una prueba que ya estaba activa termina normalmente.
La vista interior permanece inmovil mientras llega la senal. Sin el comparador
instalado, la redstone no cambia el comportamiento vanilla.

## Generadores de desafio

El generador conserva el escalado vanilla por jugadores, sus criaturas
persistentes, las recompensas por completar todas las oleadas y la variante
ominosa. Al finalizar se rearma sin el enfriamiento largo de un solo uso, pero
los jugadores deben salir del radio y volver a entrar para iniciar otra prueba.
La deteccion usa la distancia y admite jugadores en creativo, por lo que una
entidad configurada desde creativo puede iniciar la prueba sin requerir linea de
vision directa.

Cuando no queda ningun participante original en el radio, la prueba y sus
temporizadores se suspenden; las criaturas que abandonan el area de seguimiento
se eliminan o dejan de contar. Nunca puede comenzar otra prueba mientras siga
activa la anterior.

## Arcas

Las arcas no guardan de forma permanente los UUID de quienes recibieron una
recompensa. Tras completar su ciclo pueden abrirse de nuevo siempre que se use
otra llave. Un arca ominosa recogida deriva su nombre del nombre localizado de
Minecraft (`Arca Ominosa` / `Ominous Vault`) y conserva su variante al
recolocarse.

La advertencia de datos protegidos se oculta exclusivamente en generadores,
generadores de desafio y arcas obtenidos mediante Toque de Seda II. Los objetos
equivalentes creados por comandos sin la marca de Trading Cells mantienen la
advertencia vanilla.

No se incluyen fuego, fuego de almas, portales del Nether ni hielo escarchado:
no poseen un objeto de bloque colocable. El portal del Nether tampoco es un
bloque minable normal. Los bloques que ya recoge Toque de Seda I conservan su
comportamiento vanilla.
