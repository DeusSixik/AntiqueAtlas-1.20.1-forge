package hunternif.mc.impl.atlas.item;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.identity.AtlasIdentityService;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkersData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 2 or more atlases combine into one with all biome and marker data copied.
 * All data is copied into a new atlas instance.
 *
 * @author Hunternif
 */
public class RecipeAtlasCombining extends CustomRecipe {
    private static final String TAG_PENDING_ATLAS_IDS = "aaPendingCombineAtlasIds";

    public RecipeAtlasCombining(ResourceLocation pId, CraftingBookCategory craftingBookCategory) {
    	super(pId, craftingBookCategory);
    }

    @Override
    public String getGroup() {
        return AntiqueAtlas.ID + ":atlas_combine";
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        if (!AtlasIdentityService.isItemAtlasEnabled()) {
            return false;
        }
        return matches(inv);
    }

    private boolean matches(CraftingContainer inv) {
        int atlasesFound = 0;
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() != AntiqueAtlasItems.Items.ATLAS) {
                    return false;
                }
                atlasesFound++;
            }
        }
        return atlasesFound > 1;
    }
    
    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess provider) {
        if (!AtlasIdentityService.isItemAtlasEnabled()) {
            return ItemStack.EMPTY;
        }

        ItemStack firstAtlas = ItemStack.EMPTY;
        Set<Integer> atlasIds = new LinkedHashSet<>(9);
        int atlasCount = 0;
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (!(stack.getItem() instanceof AtlasItem)) {
                    return ItemStack.EMPTY;
                }
                if (firstAtlas.isEmpty()) {
                    firstAtlas = stack;
                }
                atlasCount++;
                atlasIds.add(AtlasItem.getAtlasID(stack));
            }
        }
        if (firstAtlas.isEmpty() || atlasCount < 2 || atlasIds.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = firstAtlas.copy();
        setPendingAtlasIds(result, atlasIds);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }
    
    @Override
    public ItemStack getResultItem(RegistryAccess provider) {
        return new ItemStack(AntiqueAtlasItems.Items.ATLAS);
    }

//    @Override
//    public ResourceLocation getId() {
//        return id;
//    }

	@Override
	public CraftingBookCategory category() {
		return CraftingBookCategory.MISC;
	}

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AntiqueAtlasItems.Recipes.COMBINE;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    public static boolean hasPendingCombination(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(TAG_PENDING_ATLAS_IDS);
    }

    public static void finalizeCombination(Level world, ItemStack result) {
        if (world.isClientSide || result.isEmpty() || !hasPendingCombination(result) || !AtlasIdentityService.isItemAtlasEnabled()) {
            return;
        }

        List<Integer> atlasIds = getPendingAtlasIds(result);
        clearPendingAtlasIds(result);
        if (atlasIds.isEmpty()) {
            return;
        }

        // Until the first update, on the client the returned atlas ID is the same as the first Atlas on the crafting grid.
        int atlasID = AntiqueAtlas.getAtlasDirectoryData(world).getNextAtlasId();
        int namingSourceAtlasId = atlasIds.get(0);

        AtlasData destBiomes = AntiqueAtlas.tileData.getData(atlasID, world);
        destBiomes.setDirty();
        MarkersData destMarkers = AntiqueAtlas.markersData.getMarkersData(atlasID, world);
        destMarkers.setDirty();
        for (int sourceAtlasId : atlasIds) {
            AtlasData srcBiomes = AntiqueAtlas.tileData.getData(sourceAtlasId, world);
            if (destBiomes != null && srcBiomes != null && destBiomes != srcBiomes) {
                for (ResourceKey<Level> worldRegistryKey : srcBiomes.getVisitedWorlds()) {
                    destBiomes.getWorldData(worldRegistryKey).addData(srcBiomes.getWorldData(worldRegistryKey));
                }
            }
            MarkersData srcMarkers = AntiqueAtlas.markersData.getMarkersData(sourceAtlasId, world);
            if (destMarkers != null && srcMarkers != null && destMarkers != srcMarkers) {
                for (ResourceKey<Level> worldRegistryKey : srcMarkers.getVisitedDimensions()) {
                    for (Marker marker : srcMarkers.getMarkersDataInWorld(worldRegistryKey).getAllMarkers()) {
                        destMarkers.createAndSaveMarker(marker.getType(),
                                worldRegistryKey, marker.getX(), marker.getZ(), marker.isVisibleAhead(), marker.getLabel());
                    }
                }
            }
        }

        // Set atlas ID last, because otherwise we wouldn't be able copy the
        // data from the atlas which was used as a placeholder for the result.
        AtlasIdentityService.syncAtlasNameFromStack(result, world);
        AtlasIdentityService.copyAtlasName(world, namingSourceAtlasId, atlasID);
        AntiqueAtlasItems.Components.ATLAS_ID_DATA.setData(result, new AntiqueAtlasItems.AtlasId(atlasID));
        AtlasIdentityService.initializeAtlasName(result, world, atlasID);
    }

    private static void setPendingAtlasIds(ItemStack stack, Set<Integer> atlasIds) {
        int[] serialized = atlasIds.stream().mapToInt(Integer::intValue).toArray();
        stack.getOrCreateTag().putIntArray(TAG_PENDING_ATLAS_IDS, serialized);
    }

    private static List<Integer> getPendingAtlasIds(ItemStack stack) {
        int[] serialized = stack.getTag() == null ? new int[0] : stack.getTag().getIntArray(TAG_PENDING_ATLAS_IDS);
        List<Integer> atlasIds = new ArrayList<>(serialized.length);
        for (int atlasId : serialized) {
            if (!atlasIds.contains(atlasId)) {
                atlasIds.add(atlasId);
            }
        }
        return atlasIds;
    }

    private static void clearPendingAtlasIds(ItemStack stack) {
        if (stack.getTag() == null) {
            return;
        }
        stack.removeTagKey(TAG_PENDING_ATLAS_IDS);
    }
}
