package hunternif.mc.impl.atlas.service;

import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.core.scaning.ITileDetector;
import hunternif.mc.impl.atlas.core.scaning.TileHeightType;
import hunternif.mc.impl.atlas.rules.TileSelectionContext;
import hunternif.mc.impl.atlas.rules.TileSelectionRules;
import hunternif.mc.impl.atlas.rules.TileSelectionSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.Set;

import java.util.function.Function;

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
        ResourceLocation dimensionId = world.dimension().location();
        long worldSeed = world instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
        ITileDetector detector = detectorResolver.apply(world.dimension());
        ResourceLocation rawBiomeTile = chunk == null ? null : detector.getBiomeID(world, chunk);

        TileSelectionContext globalContext = createContext(
                world,
                detector,
                TileSelectionSource.GLOBAL,
                globalTile,
                globalTile,
                rawBiomeTile,
                chunkX,
                chunkZ,
                chunk,
                worldSeed
        );
        ResourceLocation resolvedGlobalTile = rules.resolveOutputTile(globalContext);
        ResourceLocation bestTile = resolvedGlobalTile;
        int bestPriority = rules.getPriority(globalContext);

        if (rawBiomeTile != null) {
            TileSelectionContext biomeContext = createContext(
                    world,
                    detector,
                    TileSelectionSource.BIOME,
                    rawBiomeTile,
                    globalTile,
                    rawBiomeTile,
                    chunkX,
                    chunkZ,
                    chunk,
                    worldSeed
            );
            ResourceLocation biomeTile = rules.resolveOutputTile(biomeContext);
            int biomePriority = rules.getPriority(biomeContext);
            if (biomePriority > bestPriority) {
                bestTile = biomeTile;
                bestPriority = biomePriority;
            }
        }

        return bestPriority == Integer.MIN_VALUE ? null : bestTile;
    }

    private TileSelectionContext createContext(Level world,
                                               ITileDetector detector,
                                               TileSelectionSource source,
                                               ResourceLocation candidateTileId,
                                               ResourceLocation globalTileId,
                                               ResourceLocation biomeTileId,
                                               int chunkX,
                                               int chunkZ,
                                               ChunkAccess chunk,
                                               long worldSeed) {
        Integer averageSurfaceY = chunk == null ? null : getAverageSurfaceY(chunk);
        TileHeightType surfaceHeightType = averageSurfaceY == null ? null : TileHeightType.fromSurfaceY(averageSurfaceY, world.getSeaLevel());
        Set<ResourceLocation> adjacentBiomeTiles = collectAdjacentBiomeTiles(world, detector, chunkX, chunkZ);
        Set<ResourceLocation> adjacentGlobalTiles = collectAdjacentGlobalTiles(world, chunkX, chunkZ);
        return new TileSelectionContext(
                source,
                candidateTileId,
                world.dimension().location(),
                globalTileId,
                biomeTileId,
                averageSurfaceY,
                surfaceHeightType,
                adjacentBiomeTiles,
                adjacentGlobalTiles,
                worldSeed,
                chunkX,
                chunkZ
        );
    }

    private Integer getAverageSurfaceY(ChunkAccess chunk) {
        int total = 0;
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                total += chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING).getFirstAvailable(x, z);
                count++;
            }
        }
        return count == 0 ? null : Math.round((float) total / (float) count);
    }

    private Set<ResourceLocation> collectAdjacentBiomeTiles(Level world, ITileDetector detector, int chunkX, int chunkZ) {
        Set<ResourceLocation> tiles = new HashSet<>();
        forEachAdjacentChunk(chunkX, chunkZ, (neighborX, neighborZ) -> {
            ChunkAccess neighborChunk = getLoadedChunk(world, neighborX, neighborZ);
            if (neighborChunk == null) {
                return;
            }
            ResourceLocation neighborTile = detector.getBiomeID(world, neighborChunk);
            if (neighborTile != null) {
                tiles.add(neighborTile);
            }
        });
        return tiles;
    }

    private Set<ResourceLocation> collectAdjacentGlobalTiles(Level world, int chunkX, int chunkZ) {
        Set<ResourceLocation> tiles = new HashSet<>();
        forEachAdjacentChunk(chunkX, chunkZ, (neighborX, neighborZ) -> {
            ResourceLocation neighborTile = AtlasAPI.getTileAPI().getGlobalTile(world, neighborX, neighborZ);
            if (neighborTile != null) {
                tiles.add(neighborTile);
            }
        });
        return tiles;
    }

    private ChunkAccess getLoadedChunk(Level world, int chunkX, int chunkZ) {
        if (!world.getChunkSource().hasChunk(chunkX, chunkZ)) {
            return null;
        }
        return world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }

    private void forEachAdjacentChunk(int chunkX, int chunkZ, ChunkConsumer consumer) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                consumer.accept(chunkX + dx, chunkZ + dz);
            }
        }
    }

    @FunctionalInterface
    private interface ChunkConsumer {
        void accept(int chunkX, int chunkZ);
    }
}
