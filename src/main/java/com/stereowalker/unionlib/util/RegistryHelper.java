package com.stereowalker.unionlib.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class RegistryHelper {
    private RegistryHelper() {
    }

    public static ItemRegistry items() {
        return ItemRegistry.INSTANCE;
    }

    public enum ItemRegistry {
        INSTANCE;

        public Item get(ResourceLocation id) {
            return BuiltInRegistries.ITEM.get(id);
        }
    }
}
