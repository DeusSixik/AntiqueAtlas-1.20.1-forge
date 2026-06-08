package hunternif.mc.impl.atlas.structure;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class StructurePieceTile {

    private final List<ResourceLocation> tiles;
    private final int priority;
    private final StructureHandler.Setter setter;

    public StructurePieceTile(List<ResourceLocation> tiles, int priority, StructureHandler.Setter setter) {
        this.tiles = List.copyOf(tiles);
        this.priority = priority;
        this.setter = setter;
    }

    public ResourceLocation getTile() {
        return tiles.get(0);
    }

    public List<ResourceLocation> getTiles() {
        return tiles;
    }

    public ResourceLocation getTileX() {
        return getTile();
    }

    public List<ResourceLocation> getTilesX() {
        return tiles;
    }

    public ResourceLocation getTileZ() {
        return getTile();
    }

    public List<ResourceLocation> getTilesZ() {
        return tiles;
    }

    public StructureHandler.Setter getSetter() {
        return setter;
    }

    public StructureHandler.Setter getSetterX() {
        return setter;
    }

    public StructureHandler.Setter getSetterZ() {
        return setter;
    }

    public int getPriority() {
        return priority;
    }
}
