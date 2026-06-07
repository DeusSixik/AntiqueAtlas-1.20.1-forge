package hunternif.mc.impl.atlas.identity;

public record AtlasReference(AtlasReferenceType type, int atlasId) {
    public static AtlasReference player(int atlasId) {
        return new AtlasReference(AtlasReferenceType.PLAYER, atlasId);
    }

    public static AtlasReference item(int atlasId) {
        return new AtlasReference(AtlasReferenceType.ITEM, atlasId);
    }
}
