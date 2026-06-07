package hunternif.mc.impl.atlas.structure;

import net.minecraft.resources.ResourceLocation;

public class StructurePieceTileXZ extends StructurePieceTile {
    private final ResourceLocation tileZ;
    private final StructureHandler.Setter setterZ;

    public StructurePieceTileXZ(ResourceLocation tileX, StructureHandler.Setter setterX, ResourceLocation tileZ, StructureHandler.Setter setterZ, int priority) {
        super(tileX, priority, setterX);
        this.tileZ = tileZ;
        this.setterZ = setterZ;
    }

    @Override
    public ResourceLocation getTileZ() {
        return tileZ;
    }

    @Override
    public StructureHandler.Setter getSetterZ() {
        return setterZ;
    }
}
