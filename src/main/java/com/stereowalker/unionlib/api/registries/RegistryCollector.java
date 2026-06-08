package com.stereowalker.unionlib.api.registries;

import com.stereowalker.unionlib.core.registries.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RegistryCollector {
    private final IEventBus modBus;
    private final String modId;
    private final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> deferredRegisters = new HashMap<>();

    public RegistryCollector(IEventBus modBus, String modId) {
        this.modBus = modBus;
        this.modId = modId;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void addRegistryHolder(ResourceKey<? extends Registry<T>> registryKey, Class<?> holderClass) {
        DeferredRegister<T> register = (DeferredRegister<T>) deferredRegisters.computeIfAbsent(
                registryKey,
                key -> DeferredRegister.create((ResourceKey) key, modId)
        );

        for (Field field : holderClass.getDeclaredFields()) {
            RegistryObject annotation = field.getAnnotation(RegistryObject.class);
            if (annotation == null) {
                continue;
            }

            field.setAccessible(true);
            Supplier<T> supplier = () -> {
                try {
                    return (T) field.get(null);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to register " + field, e);
                }
            };
            register.register(annotation.value(), supplier);
        }
    }

    public void registerAll() {
        for (DeferredRegister<?> register : deferredRegisters.values()) {
            register.register(modBus);
        }
    }
}
