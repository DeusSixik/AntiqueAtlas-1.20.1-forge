package com.stereowalker.unionlib.resource;

import net.minecraft.resources.ResourceLocation;

public interface ReloadListener {
    default ResourceLocation id() {
        return null;
    }
}
