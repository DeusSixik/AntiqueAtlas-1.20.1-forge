package com.stereowalker.unionlib.api.creativetabs;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

public class CreativeTabPopulator {
    private final BuildCreativeModeTabContentsEvent event;

    public CreativeTabPopulator(BuildCreativeModeTabContentsEvent event) {
        this.event = event;
    }

    public boolean isToolTab() {
        return event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES;
    }

    public void addItems(ItemLike... items) {
        for (ItemLike item : items) {
            event.accept(item);
        }
    }
}
