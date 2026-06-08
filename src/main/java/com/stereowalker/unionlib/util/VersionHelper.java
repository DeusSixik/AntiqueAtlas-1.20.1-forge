package com.stereowalker.unionlib.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class VersionHelper {
    private VersionHelper() {
    }

    public static ResourceLocation toLoc(String value) {
        return ResourceLocation.tryParse(value);
    }

    public static ResourceLocation toLoc(String namespace, String path) {
        return ResourceLocation.tryBuild(namespace, path);
    }

    public static final class Data<T> {
        private final Predicate<ItemStack> hasData;
        private final Function<ItemStack, T> getter;
        private final BiConsumer<ItemStack, T> setter;
        private final java.util.function.Consumer<ItemStack> remover;

        public Data(Predicate<ItemStack> hasData,
                    Function<ItemStack, T> getter,
                    BiConsumer<ItemStack, T> setter,
                    java.util.function.Consumer<ItemStack> remover) {
            this.hasData = hasData;
            this.getter = getter;
            this.setter = setter;
            this.remover = remover;
        }

        public boolean hasData(ItemStack stack) {
            return hasData.test(stack);
        }

        public T getData(ItemStack stack) {
            return getter.apply(stack);
        }

        public void setData(ItemStack stack, T value) {
            setter.accept(stack, value);
        }

        public void removeData(ItemStack stack) {
            remover.accept(stack);
        }
    }
}
