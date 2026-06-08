package hunternif.mc.impl.atlas;

import com.stereowalker.unionlib.api.collectors.InsertCollector;
import com.stereowalker.unionlib.api.collectors.OverlayCollector;
import com.stereowalker.unionlib.api.collectors.OverlayCollector.Order;
import com.stereowalker.unionlib.api.keymaps.KeyMappingCollector;
import com.stereowalker.unionlib.client.gui.screens.config.ConfigScreen;
import com.stereowalker.unionlib.insert.ClientInserts;
import com.stereowalker.unionlib.mod.ClientSegment;
import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.client.KeyHandler;
import hunternif.mc.impl.atlas.client.OverlayRenderer;
import hunternif.mc.impl.atlas.client.gui.ExportProgressOverlay;
import hunternif.mc.impl.atlas.client.gui.GuiAtlas;
import hunternif.mc.impl.atlas.identity.AtlasReference;
import hunternif.mc.impl.atlas.identity.AtlasIdentityService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class AntiqueAtlasClientSegment extends ClientSegment {

    private static GuiAtlas guiAtlas;

    public static GuiAtlas getAtlasGUI() {
        if (guiAtlas == null) {
            guiAtlas = new GuiAtlas();
            guiAtlas.setMapScale(AntiqueAtlas.CONFIG.defaultScale);
        }
        return guiAtlas;
    }

    public static void openAtlasGUI(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        AtlasIdentityService.resolveAtlasReference(mc.player, stack)
                .ifPresent(reference -> openAtlasScreen(getAtlasGUI().prepareToOpen(reference, stack)));
    }

    public static void openAtlasGUI() {
        if (!AtlasIdentityService.isPlayerAtlasEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        AtlasIdentityService.resolveAtlasReference(mc.player, ItemStack.EMPTY)
                .ifPresent(reference -> openAtlasScreen(getAtlasGUI().prepareToOpen(reference)));
    }

    public static void openAtlasLayer() {
        if (!AtlasIdentityService.isPlayerAtlasEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || mc.player == null) {
            return;
        }

        AtlasIdentityService.resolveAtlasReference(mc.player, ItemStack.EMPTY).ifPresent(reference -> {
            GuiAtlas gui = getAtlasGUI().prepareToOpen(reference);
            gui.updateL18n();
            mc.pushGuiLayer(gui);
        });
    }

    private static void openAtlasScreen(GuiAtlas gui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            gui.updateL18n();
            mc.setScreen(gui);
        }
    }

	@Override
	public ResourceLocation getModIcon() {
		return VersionHelper.toLoc(AntiqueAtlas.ID, "pack.png");
	}
	
	@Override
	public void setupKeymappings(KeyMappingCollector collector) {
		if (AtlasIdentityService.isPlayerAtlasEnabled()) {
			collector.addKeyMapping(KeyHandler.ATLAS_KEYMAPPING);
		}
	}

	@Override
	public Screen getConfigScreen(Minecraft mc, Screen previousScreen) {
		return new ConfigScreen(previousScreen, AntiqueAtlas.CONFIG);
	}
	
	@Override
	public void registerInserts(InsertCollector collector) {
		collector.addInsert(ClientInserts.CLIENT_TICK_FINISH, ()->{
			if (AtlasIdentityService.isPlayerAtlasEnabled()) {
				KeyHandler.onClientTick(Minecraft.getInstance());
			}
		});
	}
	
	private OverlayRenderer atlasOverlayRenderer = new OverlayRenderer();
	@Override
	public void setupGuiOverlays(OverlayCollector collector) {
		collector.register("atlas", Order.END, (gui, renderer, scaledWidth, scaledHeight) -> {
			if (Minecraft.getInstance().screen == null && AntiqueAtlas.CONFIG.minimap) {
				float height = ((218f /* 0.2f*/) / scaledHeight);
				float width = ((310f /* 0.2f*/) / scaledWidth);
				renderer.poseStack().pushPose();
				renderer.poseStack().scale(54.5f / 218f, 77.5f / 310f, -1f);
				atlasOverlayRenderer.drawOverlay(renderer.poseStack(), renderer.guiGraphics().bufferSource(), 15728880, Minecraft.getInstance().player.getMainHandItem());
				renderer.poseStack().popPose();
			}
		});
	}
}
