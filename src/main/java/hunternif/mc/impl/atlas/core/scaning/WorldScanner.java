package hunternif.mc.impl.atlas.core.scaning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.core.ITileStorage;
import hunternif.mc.impl.atlas.core.TileInfo;
import hunternif.mc.impl.atlas.service.TileSelectionService;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public class WorldScanner {

    /**
     * Maps dimension ID to biomeAnalyzer.
     */
    private final Map<ResourceKey<Level>, ITileDetector> biomeAnalyzers = new HashMap<>();
    private final TileDetectorBase tileDetectorOverworld = new TileDetectorBase();
    private final TileSelectionService tileSelectionService = new TileSelectionService(this::getBiomeDetectorForWorld, AntiqueAtlas.tileSelectionRules);

    public WorldScanner() {
        setBiomeDetectorForWorld(Level.OVERWORLD, tileDetectorOverworld);
        setBiomeDetectorForWorld(Level.NETHER, new TileDetectorNether());
        setBiomeDetectorForWorld(Level.END, new TileDetectorEnd());
    }


    /**
     * If not found, returns the analyzer for overworld.
     */
    private ITileDetector getBiomeDetectorForWorld(ResourceKey<Level> world) {
        ITileDetector biomeAnalyzer = biomeAnalyzers.get(world);

        return biomeAnalyzer == null ? tileDetectorOverworld : biomeAnalyzer;
    }

    private void setBiomeDetectorForWorld(ResourceKey<Level> world, ITileDetector biomeAnalyzer) {
        biomeAnalyzers.put(world, biomeAnalyzer);
    }

    /**
     * Updates map data around player
     *
     * @return A set of the new tiles, mostly so the server can sync those with relevant clients.
     */
    public Collection<TileInfo> updateAtlasAroundPlayer(AtlasData data, Player player) {
        // Update the actual map only so often:
        int newScanInterval = Math.round(AntiqueAtlas.CONFIG.newScanInterval * 20);

        if (player.getCommandSenderWorld().getGameTime() % newScanInterval != 0) {
            return Collections.emptyList(); //no new tiles
        }

        ArrayList<TileInfo> updatedTiles = new ArrayList<>();

        int rescanInterval = newScanInterval * AntiqueAtlas.CONFIG.rescanRate;
        boolean rescanRequired = AntiqueAtlas.CONFIG.doRescan && player.getCommandSenderWorld().getGameTime() % rescanInterval == 0;

        int scanRadius = tileSelectionService.getScanRadius(player.getCommandSenderWorld());

        // Look at chunks around in a circular area:
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                if (dx * dx + dz * dz > scanRadius * scanRadius) {
                    continue; // Outside the circle
                }

                int chunkX = player.chunkPosition().x + dx;
                int chunkZ = player.chunkPosition().z + dz;

                TileInfo update = updateAtlasForChunk(data, player.getCommandSenderWorld(), chunkX, chunkZ, rescanRequired);
                if (update != null) {
                    updatedTiles.add(update);
                }
            }
        }
        return updatedTiles;
    }

    private TileInfo updateAtlasForChunk(AtlasData data, Level world, int x, int z, boolean rescanRequired) {
        ITileStorage storedData = data.getWorldData(world.dimension());
        ResourceLocation oldTile = storedData.getTile(x, z);

        ResourceLocation globalTile = AtlasAPI.getTileAPI().getGlobalTile(world, x, z);
        if (oldTile != null && !rescanRequired && globalTile == null) {
            return null;
        }

        ChunkAccess chunk = null;
        if (world.getChunkSource().hasChunk(x, z)) {
            // TODO FABRIC: forceChunkLoading crashes here
            chunk = world.getChunk(x, z, ChunkStatus.FULL, AntiqueAtlas.CONFIG.forceChunkLoading);
        }

        if (chunk == null && globalTile == null) {
            return null;
        }

        ResourceLocation tile = tileSelectionService.selectTile(world, x, z, globalTile, chunk);
        if (oldTile != null) {
            if (tile == null) {
                // If the new tile is empty, remove the old one:
                data.removeTile(world.dimension(), x, z);
            } else if (!oldTile.equals(tile)) {
                // Only update if the old tile's biome ID doesn't match the new one:
                data.setTile(world.dimension(), x, z, tile);
                return new TileInfo(x, z, tile);
            }
        } else {
            // Scanning new chunk:
            if (tile != null) {
                data.setTile(world.dimension(), x, z, tile);
                return new TileInfo(x, z, tile);
            }
        }

        return null;
    }
}
