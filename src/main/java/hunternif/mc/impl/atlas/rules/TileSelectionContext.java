package hunternif.mc.impl.atlas.rules;

import hunternif.mc.impl.atlas.core.scaning.TileHeightType;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record TileSelectionContext(
        TileSelectionSource source,
        ResourceLocation candidateTileId,
        ResourceLocation dimensionId,
        ResourceLocation globalTileId,
        ResourceLocation biomeTileId,
        Integer averageSurfaceY,
        TileHeightType surfaceHeightType,
        Set<ResourceLocation> adjacentBiomeTiles,
        Set<ResourceLocation> adjacentGlobalTiles,
        long worldSeed,
        int chunkX,
        int chunkZ
) {
    public TileSelectionContext {
        adjacentBiomeTiles = adjacentBiomeTiles == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(adjacentBiomeTiles));
        adjacentGlobalTiles = adjacentGlobalTiles == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(adjacentGlobalTiles));
    }

    public static TileSelectionContext basic(TileSelectionSource source, ResourceLocation tileId, ResourceLocation dimensionId) {
        return new TileSelectionContext(source, tileId, dimensionId, null, null, null, null,
                Collections.emptySet(), Collections.emptySet(), 0L, 0, 0);
    }
}
