package hunternif.mc.impl.atlas.identity;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.item.AtlasItem;
import hunternif.mc.impl.atlas.network.packet.s2c.play.SyncAtlasNameS2CPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class AtlasIdentityService {
    private static final String TAG_SYNCED_NAME = "aaAtlasSyncedName";
    private static Integer clientPlayerAtlasId;
    private static final Map<Integer, String> clientAtlasNames = new HashMap<>();

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

    public static void setClientPlayerAtlasState(int atlasId, String atlasName) {
        clientPlayerAtlasId = atlasId;
        setClientAtlasName(atlasId, atlasName);
    }

    public static void setClientPlayerAtlasId(int atlasId) {
        setClientPlayerAtlasState(atlasId, null);
    }

    public static void setClientAtlasName(int atlasId, String atlasName) {
        String normalizedName = normalizeName(atlasName);
        if (normalizedName == null) {
            clientAtlasNames.remove(atlasId);
            return;
        }
        clientAtlasNames.put(atlasId, normalizedName);
    }

    public static Optional<String> getClientAtlasName(int atlasId) {
        return Optional.ofNullable(clientAtlasNames.get(atlasId));
    }

    public static void clearClientState() {
        clientPlayerAtlasId = null;
        clientAtlasNames.clear();
    }

    public static void refreshClientAtlasStacks(Player player, int atlasId) {
        if (player == null) {
            return;
        }

        refreshClientAtlasStacks(player.getInventory().items, atlasId);
        refreshClientAtlasStacks(player.getInventory().armor, atlasId);
        refreshClientAtlasStacks(player.getInventory().offhand, atlasId);
        refreshClientAtlasSlots(player.inventoryMenu.slots, atlasId);
        refreshClientAtlasSlots(player.containerMenu.slots, atlasId);
        refreshClientAtlasStack(player.containerMenu.getCarried(), atlasId);
    }

    public static OptionalInt resolveAtlasId(Player player, ItemStack stack) {
        if (isAtlasItem(stack)) {
            if (!isItemAtlasEnabled()) {
                return OptionalInt.empty();
            }
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

    public static Optional<AtlasReference> resolveAtlasReference(Player player, ItemStack stack) {
        if (isAtlasItem(stack)) {
            if (!isItemAtlasEnabled()) {
                return Optional.empty();
            }
            return Optional.of(AtlasReference.item(AtlasItem.getAtlasID(stack)));
        }
        if (player != null && isPlayerAtlasEnabled()) {
            if (player.level().isClientSide()) {
                return clientPlayerAtlasId == null ? Optional.empty() : Optional.of(AtlasReference.player(clientPlayerAtlasId));
            }
            return Optional.of(AtlasReference.player(getOrCreatePlayerAtlasId(player)));
        }
        return Optional.empty();
    }

    public static Optional<String> getAtlasName(Level world, int atlasId) {
        if (world.isClientSide()) {
            return getClientAtlasName(atlasId);
        }
        return AntiqueAtlas.getAtlasDirectoryData((ServerLevel) world).getAtlasName(atlasId);
    }

    public static void setAtlasName(Level world, int atlasId, String name) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean changed = AntiqueAtlas.getAtlasDirectoryData(serverLevel).setAtlasName(atlasId, name);
        if (changed) {
            syncAtlasNameToRelevantPlayers(serverLevel, atlasId);
        }
    }

    public static void copyAtlasName(Level world, int sourceAtlasId, int targetAtlasId) {
        if (!(world instanceof ServerLevel)) {
            return;
        }

        Optional<String> storedName = getAtlasName(world, sourceAtlasId);
        setAtlasName(world, targetAtlasId, storedName.orElse(null));
    }

    public static void syncAtlasNameToPlayer(ServerPlayer player, int atlasId) {
        String atlasName = getAtlasName(player.level(), atlasId).orElse(null);
        new SyncAtlasNameS2CPacket(atlasId, atlasName).send(player);
    }

    public static void syncAtlasNameToRelevantPlayers(ServerLevel world, int atlasId) {
        for (ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
            if (AntiqueAtlas.activeAtlasResolver.isAtlasActiveForPlayer(player, atlasId)) {
                syncAtlasNameToPlayer(player, atlasId);
            }
        }
    }

    public static Component getDefaultAtlasName(int atlasId) {
        return Component.translatable("item.navigate.navigation", atlasId);
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
                stack.resetHoverName();
                clearSyncedName(stack);
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
            stack.resetHoverName();
            clearSyncedName(stack);
        }
    }

    public static void copyAtlasNameState(ItemStack source, ItemStack target) {
        if (source.hasCustomHoverName()) {
            target.setHoverName(source.getHoverName());
        } else {
            target.resetHoverName();
        }

        String syncedName = getSyncedName(source);
        if (syncedName != null) {
            setSyncedName(target, syncedName);
        } else {
            clearSyncedName(target);
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

    private static void refreshClientAtlasStacks(Iterable<ItemStack> stacks, int atlasId) {
        for (ItemStack stack : stacks) {
            refreshClientAtlasStack(stack, atlasId);
        }
    }

    private static void refreshClientAtlasSlots(Iterable<Slot> slots, int atlasId) {
        for (Slot slot : slots) {
            refreshClientAtlasStack(slot.getItem(), atlasId);
        }
    }

    private static void refreshClientAtlasStack(ItemStack stack, int atlasId) {
        if (!isAtlasItem(stack) || AtlasItem.getAtlasID(stack) != atlasId) {
            return;
        }

        String atlasName = clientAtlasNames.get(atlasId);
        if (atlasName == null) {
            stack.resetHoverName();
            clearSyncedName(stack);
            return;
        }

        applyAtlasNameToStack(stack, atlasName);
    }
}
