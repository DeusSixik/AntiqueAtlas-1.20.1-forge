package hunternif.mc.impl.atlas.client;

import net.minecraft.resources.ResourceLocation;

public record TileMetadata(ResourceLocation lodGroup, int lodPriority, int lodMinCount) {
    public static final TileMetadata DEFAULT = new TileMetadata(null, 0, 1);
}
