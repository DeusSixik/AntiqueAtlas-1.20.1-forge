# Antique Atlas ![Minecraft 1.16.5](https://img.shields.io/badge/minecraft-1.16.5-blue.svg) [![Build Status](https://github.com/AntiqueAtlasTeam/AntiqueAtlas/workflows/Build%20Status/badge.svg)](https://github.com/AntiqueAtlasTeam/AntiqueAtlas/actions)
Antique Atlas is a book that acts like a map featuring infinite scrolling, zoom and custom labeled markers. The map is generated around the player by calculating the average biome in each 16x16 chunk.

Navigate the map by dragging it with the mouse, clicking arrow buttons or pressing arrow keys on the keyboard. Use the +/- keys or mouse wheel to zoom in and out.

You can export the map of the current dimension into a PNG image, see buttons on the right side of the GUI.

You can edit the configs to set which biome uses what texture, or even assign your own textures to any biome, including custom mod biomes. See tutorial on the wiki: https://github.com/Hunternif/AntiqueAtlas/wiki/Editing-Textures

### Tile selection rules
Tile selection can be customized through JSON files in `data/<namespace>/atlas/tile_selection/*.json`.

Supported rule matching now includes:
- source (`global` or `biome`)
- current tile id or prefix
- dimension
- current biome tile id or prefix
- current global tile id or prefix
- whether a global tile exists in the chunk
- average surface height and height band (`valley`, `low`, `mid`, `high`, `peak`)
- adjacent biome/global tiles from already loaded neighboring chunks

There is a full format reference with examples in `docs/tile-selection.md`.

### API
If you are a mod developer and you wish to interact with AntiqueAtlas, you will need the source code of the API. You can include the whole source code of AntiqueAtlas (the `...-sources.jar` in [Releases](https://github.com/Hunternif/AntiqueAtlas/releases)) which allows you to test the  interaction when debugging your mod.

Use the class `hunternif.mc.atlas.api.AtlasAPI` to obtain a reference to the API. There are 2 actual APIs: TileAPI and MarkerAPI. See javadocs/sources and the wiki for more: https://github.com/Hunternif/AntiqueAtlas/wiki/API
