package com.stereowalker.unionlib.api.keymaps;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

import java.util.ArrayList;
import java.util.List;

public class KeyMappingCollector {
    private final List<KeyMapping> mappings = new ArrayList<>();

    public void addKeyMapping(KeyMapping keyMapping) {
        mappings.add(keyMapping);
    }

    public void registerAll(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : mappings) {
            event.register(mapping);
        }
    }

    public boolean isEmpty() {
        return mappings.isEmpty();
    }
}
