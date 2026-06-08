package hunternif.mc.impl.atlas.core.scaning;

/**
 * The enum represents the different height levels in biomes.
 */
public enum TileHeightType {
    VALLEY("valley"),
    LOW("low"),
    MID("mid"),
    HIGH("high"),
    PEAK("peak");

    private final String name;

    TileHeightType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static TileHeightType fromName(String name) {
        for (TileHeightType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown tile height type: " + name);
    }

    public static TileHeightType fromSurfaceY(int y, int seaLevel) {
        if (y < seaLevel + 10) {
            return TileHeightType.VALLEY;
        }
        if (y < seaLevel + 20) {
            return TileHeightType.LOW;
        }
        if (y < seaLevel + 35) {
            return TileHeightType.MID;
        }
        if (y < seaLevel + 50) {
            return TileHeightType.HIGH;
        }
        return TileHeightType.PEAK;
    }

    public String toString() {
        return getName();
    }
}
