package hunternif.mc.impl.atlas.rules;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

public record TileSelectionRule(
        TileSelectionSource source,
        int priority,
        ResourceLocation tileId,
        String tilePrefix,
        Set<ResourceLocation> dimensions
) {
    public TileSelectionRule {
        dimensions = dimensions == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(dimensions));
    }

    public boolean matches(TileSelectionSource source, ResourceLocation tileId, ResourceLocation dimensionId) {
        if (this.source != source || tileId == null) {
            return false;
        }
        if (!dimensions.isEmpty() && (dimensionId == null || !dimensions.contains(dimensionId))) {
            return false;
        }
        if (this.tileId != null && !this.tileId.equals(tileId)) {
            return false;
        }
        if (tilePrefix != null && !tileId.toString().startsWith(tilePrefix)) {
            return false;
        }
        return this.tileId != null || this.tilePrefix == null || tileId.toString().startsWith(tilePrefix);
    }

    public boolean isExplicitMatcher() {
        return tileId != null || tilePrefix != null;
    }
}
