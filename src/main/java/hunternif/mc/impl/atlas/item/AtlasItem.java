package hunternif.mc.impl.atlas.item;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.identity.AtlasIdentityService;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapBanner;

public class AtlasItem extends Item {

    public AtlasItem(Item.Properties settings) {
        super(settings);
    }
    
    public static int getAtlasID(ItemStack stack) {
    	if (AntiqueAtlasItems.Components.ATLAS_ID_DATA.hasData(stack))
    		return AntiqueAtlasItems.Components.ATLAS_ID_DATA.getData(stack).id();
    	return 0;
    }

    @Override
    public Component getName(ItemStack stack) {
        return AtlasIdentityService.getDefaultAtlasName(getAtlasID(stack));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        super.onCraftedBy(stack, world, player);
        RecipeAtlasCombining.finalizeCombination(world, stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player playerEntity, InteractionHand hand) {
        ItemStack stack = playerEntity.getItemInHand(hand);

        if (world.isClientSide) {
            AntiqueAtlasClientSegment.openAtlasGUI(stack);
        }

        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide()) {
            return super.useOn(context);
        }

        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
        if (blockState.is(BlockTags.BANNERS)) {
            AntiqueAtlasClientSegment.openAtlasGUI(context.getItemInHand());
            MapBanner mapBannerMarker = MapBanner.fromWorld(context.getLevel(), context.getClickedPos());
            AntiqueAtlasClientSegment.getAtlasGUI().openMarkerFinalizer(mapBannerMarker.getName());
            context.getLevel().playSound(context.getPlayer(), context.getClickedPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1f);

            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isEquipped) {
        AtlasData data = AntiqueAtlas.tileData.getData(stack, world);
        if (data == null || !(entity instanceof Player)) return;

        int atlasId = getAtlasID(stack);

        Player player = (Player) entity;
        if (!world.isClientSide) {
            AtlasIdentityService.syncAtlasNameFromStack(stack, world);
            AntiqueAtlas.atlasScanService.syncAtlasToPlayer((net.minecraft.server.level.ServerPlayer) player, atlasId);
        }
    }

}
