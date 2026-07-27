# Informe final: Trader, Autotrader y arquitectura

## 1. Java modificado

- `VillagerTradeScreenCommon`: panel de XP compartido, orbe vanilla, barra profesional vanilla y flechas con mayor contraste.
- `VillagerTradeScreenLayout`: coordenadas comunes del panel, icono y textos de XP.
- `VillagerTradingCellScreen` y `AutotraderScreen`: usan los mismos componentes visuales y las nuevas rutas de widgets.
- `BreederScreen`: usa las siluetas de capturadores desde su carpeta compartida.
- Block entities de Autotrader, Trader, Criaderos, Incubadoras, Farmer, Convertidor y Granja de Hierro: dependen de puertos de entrada.
- `FeatureComposition`: único punto que construye los servicios e inyecta configuración.
- `CapturedMobStackAdapter`: fachada pública compartida para datos y entidades capturadas.
- `build.gradle`: comprobaciones de límites entre capas y features.

## 2. Texturas modificadas

- Fondo común: `textures/gui/trader/backgrounds/default.png`.
- Widgets: `textures/gui/trader/widgets/{arrows,rows,selector,slots}`.
- Reinicio: `textures/gui/sprites/trader/reset`.
- Capturadores vacíos: `textures/gui/sprites/captures`.
- Eliminadas la línea verde superior y las texturas antiguas sin referencias.
- Flechas habilitadas y deshabilitadas regeneradas con silueta simétrica y mayor contraste.

## 3. Métodos y tipos añadidos

- `VillagerTradeScreenCommon.drawStoredExperiencePanel`.
- `VillagerTradeScreenLayout.dropdownContentHeight`.
- Puertos `AutotraderUseCase`, `TradeCageUseCase`, `BreederUseCase`, `IncubatorUseCase`, `FarmerUseCase`, `ConverterUseCase` e `IronFarmUseCase`.
- Servicios de aplicación correspondientes y `FeatureComposition`.

## 4. Métodos y archivos eliminados

- `VillagerTradeScreenCommon.drawCost`.
- `TradeExperienceDisplay.compactLine`.
- Casos de uso antiguos de tick duplicados y adaptadores concretos expuestos entre features.
- Fondos de criadero y sprites de reinicio antiguos sin referencias.

## 5. Sincronización

- No se cambió el protocolo en este pulido visual.
- Las ofertas, revisión, selección, profesión, nivel y XP siguen llegando desde el servidor.
- Las Screens solo representan estado sincronizado.
- El Autotrader cargó sus ofertas al abrirse sin pulsar reinicio.

## 6. Coordenadas

- Menú: `348x210`.
- Slots: marco `18x18`; área real `16x16` en `+1,+1`.
- Panel XP: `x=126`, `y=77`, `108x22`.
- Orbe XP: `x=129`, `y=80`, `16x16`.
- Textos XP: `x=148`, líneas en `y=79` y `y=90`.
- Barra profesional: `x=130`, `y=41`, ancho `202`.
- Texto Inventario: `x=152`, `y=105`.

## 7. Verificación ejecutada

- `gradlew clean build`: correcto.
- `checkArchitecture`, `test` y `verifyDomainRules`: correctos.
- `generate_villager_trade_gui.py --check`: correcto.
- Mundo `Test`: cargado, Trader y Autotrader abiertos, guardado y cerrado con normalidad.
- Captura final Trader: `artifacts/trader-final.png`.
- Captura final Autotrader: `artifacts/autotrader-final.png`.
- El cliente cargó `1597` recetas y no registró texturas ausentes del mod.

## 8. Límites

- Las capturas muestran los estados normales; hover y pulsado se validaron por código y compilación.
- El mundo `Test` conserva un aviso de metadatos de un paquete externo; no procede del mod.
