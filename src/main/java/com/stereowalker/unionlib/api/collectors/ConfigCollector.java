package com.stereowalker.unionlib.api.collectors;

import hunternif.mc.impl.atlas.util.SimpleUnionConfigLoader;

public class ConfigCollector {
    private final String modId;

    public ConfigCollector(String modId) {
        this.modId = modId;
    }

    public void registerConfig(Object config) {
        SimpleUnionConfigLoader.load(modId, config);
    }
}
