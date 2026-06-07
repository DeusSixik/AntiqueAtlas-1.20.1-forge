package hunternif.mc.impl.atlas.identity;

import java.util.Optional;
import java.util.OptionalInt;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.item.AtlasItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class AtlasIdentityService {
    private static final String TAG_SYNCED_NAME = "aaAtlasSyncedName";
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

    public static Optional<String> getAtlasName(Level world, int atlasId) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return AntiqueAtlas.getAtlasDirectoryData(serverLevel).getAtlasName(atlasId);
    }

    public static void setAtlasName(Level world, int atlasId, String name) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        AntiqueAtlas.getAtlasDirectoryData(serverLevel).setAtlasName(atlasId, name);
    }

    public static Component getDefaultAtlasName(int atlasId) {
        return Component.translatable("item.antiqueatlas.antique_atlas", atlasId);
    }

    public static void syncAtlasNameFromStack(ItemStack stack, Level world) {
        if (!(world instanceof ServerLevel) || !isAtlasItem(stack)) {
            return;
        }

        int atlasId = AtlasItem.getAtlasID(stack);
        Optional<String> storedName = getAtlasName(world, atlasId);
        String syncedName = getSyncedName(stack);

        if (stack.hasCustomHoverName()) {
            String stackName = normalizeName(stack.getHoverName().getString());
            if (stackName == null) {
                stack.resetHoverName();
                clearSyncedName(stack);
                return;
            }

            if (!stackName.equals(syncedName)) {
                setAtlasName(world, atlasId, stackName);
                setSyncedName(stack, stackName);
                return;
            }

            if (storedName.isPresent()) {
                if (!stackName.equals(storedName.get())) {
                    applyAtlasNameToStack(stack, storedName.get());
                }
            } else {
                setAtlasName(world, atlasId, stackName);
            }
            return;
        }

        if (syncedName != null) {
            setAtlasName(world, atlasId, null);
            clearSyncedName(stack);
            return;
        }

        storedName.ifPresent(name -> applyAtlasNameToStack(stack, name));
    }

    public static void initializeAtlasName(ItemStack stack, Level world, int atlasId) {
        if (!(world instanceof ServerLevel) || !isAtlasItem(stack)) {
            return;
        }

        Optional<String> storedName = getAtlasName(world, atlasId);
        if (storedName.isPresent()) {
            applyAtlasNameToStack(stack, storedName.get());
        } else {
            clearSyncedName(stack);
        }
    }

    private static void applyAtlasNameToStack(ItemStack stack, String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            stack.resetHoverName();
            clearSyncedName(stack);
            return;
        }

        stack.setHoverName(Component.literal(normalized));
        setSyncedName(stack, normalized);
    }

    private static void setSyncedName(ItemStack stack, String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            clearSyncedName(stack);
            return;
        }
        stack.getOrCreateTag().putString(TAG_SYNCED_NAME, normalized);
    }

    private static void clearSyncedName(ItemStack stack) {
        if (stack.getTag() == null) {
            return;
        }
        stack.removeTagKey(TAG_SYNCED_NAME);
    }

    private static String getSyncedName(ItemStack stack) {
        if (stack.getTag() == null || !stack.getTag().contains(TAG_SYNCED_NAME)) {
            return null;
        }
        return normalizeName(stack.getTag().getString(TAG_SYNCED_NAME));
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
