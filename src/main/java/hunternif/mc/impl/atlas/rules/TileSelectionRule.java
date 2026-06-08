package hunternif.mc.impl.atlas.rules;

import hunternif.mc.impl.atlas.core.scaning.TileHeightType;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record TileSelectionRule(
        TileSelectionSource source,
        int priority,
        ResourceLocation tileId,
        String tilePrefix,
        Set<ResourceLocation> dimensions,
        Set<ResourceLocation> biomeTiles,
        String biomeTilePrefix,
        Set<ResourceLocation> globalTiles,
        String globalTilePrefix,
        Boolean hasGlobalTile,
        Set<TileHeightType> heightTypes,
        Integer minSurfaceY,
        Integer maxSurfaceY,
        Set<ResourceLocation> adjacentBiomeTilesAny,
        Set<ResourceLocation> adjacentGlobalTilesAny,
        List<ResourceLocation> outputTiles
) {
    public TileSelectionRule {
        dimensions = dimensions == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(dimensions));
        biomeTiles = biomeTiles == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(biomeTiles));
        globalTiles = globalTiles == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(globalTiles));
        adjacentBiomeTilesAny = adjacentBiomeTilesAny == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(adjacentBiomeTilesAny));
        adjacentGlobalTilesAny = adjacentGlobalTilesAny == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(adjacentGlobalTilesAny));
        heightTypes = heightTypes == null || heightTypes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(heightTypes));
        outputTiles = outputTiles == null ? Collections.emptyList() : List.copyOf(outputTiles);
    }

    public boolean matches(TileSelectionContext context) {
        if (context == null || this.source != context.source() || context.candidateTileId() == null) {
            return false;
        }
        if (!dimensions.isEmpty() && (context.dimensionId() == null || !dimensions.contains(context.dimensionId()))) {
            return false;
        }
        if (this.tileId != null && !this.tileId.equals(context.candidateTileId())) {
            return false;
        }
        if (tilePrefix != null && !context.candidateTileId().toString().startsWith(tilePrefix)) {
            return false;
        }
        if (!biomeTiles.isEmpty() && !biomeTiles.contains(context.biomeTileId())) {
            return false;
        }
        if (biomeTilePrefix != null && (context.biomeTileId() == null || !context.biomeTileId().toString().startsWith(biomeTilePrefix))) {
            return false;
        }
        if (!globalTiles.isEmpty() && !globalTiles.contains(context.globalTileId())) {
            return false;
        }
        if (globalTilePrefix != null && (context.globalTileId() == null || !context.globalTileId().toString().startsWith(globalTilePrefix))) {
            return false;
        }
        if (hasGlobalTile != null && hasGlobalTile.booleanValue() != (context.globalTileId() != null)) {
            return false;
        }
        if (!heightTypes.isEmpty() && !heightTypes.contains(context.surfaceHeightType())) {
            return false;
        }
        if (minSurfaceY != null && (context.averageSurfaceY() == null || context.averageSurfaceY() < minSurfaceY)) {
            return false;
        }
        if (maxSurfaceY != null && (context.averageSurfaceY() == null || context.averageSurfaceY() > maxSurfaceY)) {
            return false;
        }
        if (!adjacentBiomeTilesAny.isEmpty() && Collections.disjoint(adjacentBiomeTilesAny, context.adjacentBiomeTiles())) {
            return false;
        }
        if (!adjacentGlobalTilesAny.isEmpty() && Collections.disjoint(adjacentGlobalTilesAny, context.adjacentGlobalTiles())) {
            return false;
        }
        return this.tileId != null || this.tilePrefix == null || context.candidateTileId().toString().startsWith(tilePrefix);
    }

    public boolean isExplicitMatcher() {
        return tileId != null || tilePrefix != null;
    }

    public boolean hasOutputTiles() {
        return !outputTiles.isEmpty();
    }
}
