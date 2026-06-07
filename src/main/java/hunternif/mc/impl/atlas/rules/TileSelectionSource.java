package hunternif.mc.impl.atlas.rules;

public enum TileSelectionSource {
    GLOBAL,
    BIOME;

    public static TileSelectionSource fromConfigKey(String key) {
        return switch (key.toLowerCase()) {
            case "global" -> GLOBAL;
            case "biome" -> BIOME;
            default -> throw new IllegalArgumentException("Unknown tile selection source: " + key);
        };
    }
}
