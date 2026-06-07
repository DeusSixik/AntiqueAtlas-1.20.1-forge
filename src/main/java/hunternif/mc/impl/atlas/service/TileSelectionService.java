package hunternif.mc.impl.atlas.service;

import java.util.function.Function;

import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.core.scaning.ITileDetector;
import hunternif.mc.impl.atlas.rules.TileSelectionRules;
import hunternif.mc.impl.atlas.rules.TileSelectionSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

public class TileSelectionService {
    private final Function<ResourceKey<Level>, ITileDetector> detectorResolver;
    private final TileSelectionRules rules;

    public TileSelectionService(Function<ResourceKey<Level>, ITileDetector> detectorResolver, TileSelectionRules rules) {
        this.detectorResolver = detectorResolver;
        this.rules = rules;
    }

    public int getScanRadius(Level world) {
        return detectorResolver.apply(world.dimension()).getScanRadius();
    }

    public ResourceLocation selectTile(Level world, int chunkX, int chunkZ, ChunkAccess chunk) {
        return selectTile(world, chunkX, chunkZ, AtlasAPI.getTileAPI().getGlobalTile(world, chunkX, chunkZ), chunk);
    }

    public ResourceLocation selectTile(Level world, int chunkX, int chunkZ, ResourceLocation globalTile, ChunkAccess chunk) {
        ResourceLocation bestTile = globalTile;
        ResourceLocation dimensionId = world.dimension().location();
        int bestPriority = rules.getPriority(TileSelectionSource.GLOBAL, globalTile, dimensionId);

        if (chunk != null) {
            ResourceLocation biomeTile = detectorResolver.apply(world.dimension()).getBiomeID(world, chunk);
            int biomePriority = rules.getPriority(TileSelectionSource.BIOME, biomeTile, dimensionId);
            if (biomePriority > bestPriority) {
                bestTile = biomeTile;
                bestPriority = biomePriority;
            }
        }

        return bestPriority == Integer.MIN_VALUE ? null : bestTile;
    }
}
