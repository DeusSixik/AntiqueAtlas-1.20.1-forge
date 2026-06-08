package hunternif.mc.impl.atlas.mixin;

import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.identity.AtlasIdentityService;
import hunternif.mc.impl.atlas.item.AntiqueAtlasItems;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void antiqueatlas_addPlayerAtlasButton(CallbackInfo ci) {
        if (!AtlasIdentityService.isPlayerAtlasEnabled()) {
            return;
        }

        ItemStack atlasIcon = new ItemStack(AntiqueAtlasItems.Items.ATLAS);
        Tooltip tooltip = Tooltip.create(getPlayerAtlasTooltip(this.minecraft.player));
        addRenderableWidget(Button.builder(Component.empty(), button -> AntiqueAtlasClientSegment.openAtlasLayer())
                .bounds(this.leftPos + 128, this.topPos + 61, 18, 18)
                .tooltip(tooltip)
                .build(builder -> new Button(builder) {
                    @Override
                    protected void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        guiGraphics.renderItem(atlasIcon, getX() + 1, getY() + 1);
                    }
                }));
    }

    private static Component getPlayerAtlasTooltip(net.minecraft.world.entity.player.Player player) {
        Component defaultText = Component.translatable("gui.antiqueatlas.openPlayerAtlas");
        if (!(player instanceof LocalPlayer localPlayer)) {
            return defaultText;
        }

        return AtlasIdentityService.resolveAtlasId(localPlayer, ItemStack.EMPTY)
                .stream()
                .mapToObj(atlasId -> AtlasIdentityService.getAtlasName(localPlayer.level(), atlasId)
                        .<Component>map(name -> Component.empty()
                                .append(defaultText)
                                .append("\n")
                                .append(Component.translatable("gui.antiqueatlas.playerAtlasName", name)))
                        .orElse(defaultText))
                .findFirst()
                .orElse(defaultText);
    }
}
