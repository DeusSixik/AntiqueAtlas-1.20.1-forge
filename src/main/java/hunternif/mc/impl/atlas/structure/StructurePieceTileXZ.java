package hunternif.mc.impl.atlas.structure;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public class StructurePieceTileXZ extends StructurePieceTile {
    private final List<ResourceLocation> tilesZ;
    private final StructureHandler.Setter setterZ;

    public StructurePieceTileXZ(List<ResourceLocation> tilesX, StructureHandler.Setter setterX, List<ResourceLocation> tilesZ, StructureHandler.Setter setterZ, int priority) {
        super(tilesX, priority, setterX);
        this.tilesZ = List.copyOf(tilesZ);
        this.setterZ = setterZ;
    }

    @Override
    public ResourceLocation getTileZ() {
        return tilesZ.get(0);
    }

    @Override
    public List<ResourceLocation> getTilesZ() {
        return tilesZ;
    }

    @Override
    public StructureHandler.Setter getSetterZ() {
        return setterZ;
    }
}
