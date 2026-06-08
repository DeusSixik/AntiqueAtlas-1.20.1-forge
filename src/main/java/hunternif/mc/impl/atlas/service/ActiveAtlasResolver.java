package hunternif.mc.impl.atlas.service;

import hunternif.mc.impl.atlas.identity.AtlasIdentityService;
import hunternif.mc.impl.atlas.identity.AtlasReference;
import hunternif.mc.impl.atlas.item.AtlasItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActiveAtlasResolver {
    public List<AtlasReference> getActiveAtlases(Player player) {
        Map<Integer, AtlasReference> atlasesById = new LinkedHashMap<>();

        if (AtlasIdentityService.isPlayerAtlasEnabled()) {
            int playerAtlasId = AtlasIdentityService.getOrCreatePlayerAtlasId(player);
            atlasesById.put(playerAtlasId, AtlasReference.player(playerAtlasId));
        }

        if (AtlasIdentityService.isItemAtlasEnabled()) {
            collectItemAtlases(atlasesById, player.getInventory().items);
            collectItemAtlases(atlasesById, player.getInventory().offhand);
        }

        return new ArrayList<>(atlasesById.values());
    }

    public List<Integer> getActiveAtlasIds(Player player) {
        List<AtlasReference> atlases = getActiveAtlases(player);
        List<Integer> atlasIds = new ArrayList<>(atlases.size());
        for (AtlasReference atlas : atlases) {
            atlasIds.add(atlas.atlasId());
        }
        return atlasIds;
    }

    public boolean isAtlasActiveForPlayer(Player player, int atlasId) {
        for (AtlasReference atlas : getActiveAtlases(player)) {
            if (atlas.atlasId() == atlasId) {
                return true;
            }
        }
        return false;
    }

    private void collectItemAtlases(Map<Integer, AtlasReference> atlasesById, Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || !(stack.getItem() instanceof AtlasItem)) {
                continue;
            }

            int atlasId = AtlasItem.getAtlasID(stack);
            atlasesById.putIfAbsent(atlasId, AtlasReference.item(atlasId));
        }
    }
}
