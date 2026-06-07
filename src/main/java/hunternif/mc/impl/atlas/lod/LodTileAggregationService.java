package hunternif.mc.impl.atlas.lod;

import hunternif.mc.impl.atlas.core.ITileStorage;
import hunternif.mc.impl.atlas.rules.TileSelectionRules;
import hunternif.mc.impl.atlas.util.Rect;
import net.minecraft.resources.ResourceLocation;

public class LodTileAggregationService {
    private final TileSelectionRules rules;

    public LodTileAggregationService(TileSelectionRules rules) {
        this.rules = rules;
    }

    public ITileStorage createStorage(ITileStorage source, Rect scope, int step, ResourceLocation dimensionId) {
        if (step <= 1) {
            return source;
        }
        return new LodTileStorage(source, scope, step, dimensionId, rules);
    }
}
