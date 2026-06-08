package com.stereowalker.unionlib.mod;

import com.stereowalker.unionlib.api.collectors.InsertCollector;
import com.stereowalker.unionlib.api.keymaps.KeyMappingCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public class ClientSegment {
    public ResourceLocation getModIcon() {
        return null;
    }

    public void setupKeymappings(KeyMappingCollector collector) {
    }

    public Screen getConfigScreen(Minecraft mc, Screen previousScreen) {
        return previousScreen;
    }

    public void registerInserts(InsertCollector collector) {
    }
}
