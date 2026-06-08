package com.stereowalker.unionlib.forge;

import net.minecraftforge.fml.loading.FMLEnvironment;

public final class PlatformHelper {
    private PlatformHelper() {
    }

    public static boolean isDevEnvironment() {
        return !FMLEnvironment.production;
    }
}
