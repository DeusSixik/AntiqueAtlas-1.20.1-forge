package hunternif.mc.impl.atlas.identity;

import java.util.OptionalInt;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.item.AtlasItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class AtlasIdentityService {
    private static Integer clientPlayerAtlasId;

    private AtlasIdentityService() {
    }

    public static boolean isItemAtlasEnabled() {
        return AntiqueAtlas.CONFIG.enableItemAtlas;
    }

    public static boolean isPlayerAtlasEnabled() {
        return AntiqueAtlas.CONFIG.enablePlayerAtlas;
    }

    public static int getOrCreatePlayerAtlasId(Player player) {
        if (player.level().isClientSide()) {
            if (clientPlayerAtlasId == null) {
                throw new IllegalStateException("Player atlas id has not been synced to the client yet");
            }
            return clientPlayerAtlasId;
        }

        return AntiqueAtlas.getAtlasDirectoryData(player.level()).getOrCreatePlayerAtlasId(player.getUUID());
    }

    public static boolean isAtlasItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof AtlasItem;
    }

    public static void setClientPlayerAtlasId(int atlasId) {
        clientPlayerAtlasId = atlasId;
    }

    public static void clearClientState() {
        clientPlayerAtlasId = null;
    }

    public static OptionalInt resolveAtlasId(Player player, ItemStack stack) {
        if (isAtlasItem(stack)) {
            return OptionalInt.of(AtlasItem.getAtlasID(stack));
        }
        if (player != null && isPlayerAtlasEnabled()) {
            if (player.level().isClientSide()) {
                return clientPlayerAtlasId == null ? OptionalInt.empty() : OptionalInt.of(clientPlayerAtlasId);
            }
            return OptionalInt.of(getOrCreatePlayerAtlasId(player));
        }
        return OptionalInt.empty();
    }
}
