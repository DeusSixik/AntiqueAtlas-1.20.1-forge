package hunternif.mc.impl.atlas.structure;

import net.minecraft.resources.ResourceLocation;

public class StructurePieceTile {

    private final ResourceLocation tile;
    private final int priority;
    private final StructureHandler.Setter setter;

    public StructurePieceTile(ResourceLocation tile, int priority, StructureHandler.Setter setter) {
        this.tile = tile;
        this.priority = priority;
        this.setter = setter;
    }

    public ResourceLocation getTile() {
        return tile;
    }

    public ResourceLocation getTileX() {
        return tile;
    }

    public ResourceLocation getTileZ() {
        return tile;
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
