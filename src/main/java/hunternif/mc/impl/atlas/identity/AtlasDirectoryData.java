package hunternif.mc.impl.atlas.identity;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Shared atlas directory for the whole save.
 * Stores the next free atlas id and player-to-atlas bindings.
 */
public class AtlasDirectoryData extends SavedData {
    private static final String TAG_NEXT_ID = "aaNextID";
    private static final String TAG_PLAYER_ATLASES = "aaPlayerAtlases";
    private static final String TAG_PLAYER_UUID = "playerUUID";
    private static final String TAG_ATLAS_ID = "atlasID";

    private int nextId = 1;
    private final Map<UUID, Integer> playerAtlasIds = new HashMap<>();

    public static AtlasDirectoryData fromNbt(CompoundTag compound) {
        AtlasDirectoryData data = new AtlasDirectoryData();
        if (compound.contains(TAG_NEXT_ID, Tag.TAG_ANY_NUMERIC)) {
            data.nextId = compound.getInt(TAG_NEXT_ID);
        }

        ListTag entries = compound.getList(TAG_PLAYER_ATLASES, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.hasUUID(TAG_PLAYER_UUID) || !entry.contains(TAG_ATLAS_ID, Tag.TAG_ANY_NUMERIC)) {
                continue;
            }
            data.playerAtlasIds.put(entry.getUUID(TAG_PLAYER_UUID), entry.getInt(TAG_ATLAS_ID));
        }

        return data;
    }

    public int getNextAtlasId() {
        int id = nextId++;
        setDirty();
        return id;
    }

    public OptionalInt getPlayerAtlasId(UUID playerId) {
        Integer atlasId = playerAtlasIds.get(playerId);
        return atlasId == null ? OptionalInt.empty() : OptionalInt.of(atlasId);
    }

    public int getOrCreatePlayerAtlasId(UUID playerId) {
        Integer atlasId = playerAtlasIds.get(playerId);
        if (atlasId != null) {
            return atlasId;
        }

        int newAtlasId = getNextAtlasId();
        playerAtlasIds.put(playerId, newAtlasId);
        setDirty();
        return newAtlasId;
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        compound.putInt(TAG_NEXT_ID, nextId);

        ListTag entries = new ListTag();
        for (Map.Entry<UUID, Integer> entry : playerAtlasIds.entrySet()) {
            CompoundTag playerAtlas = new CompoundTag();
            playerAtlas.putUUID(TAG_PLAYER_UUID, entry.getKey());
            playerAtlas.putInt(TAG_ATLAS_ID, entry.getValue());
            entries.add(playerAtlas);
        }
        compound.put(TAG_PLAYER_ATLASES, entries);
        return compound;
    }
}
