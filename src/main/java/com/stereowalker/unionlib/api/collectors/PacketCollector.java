package com.stereowalker.unionlib.api.collectors;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public class PacketCollector {
    private final SimpleChannel channel;
    private int nextId = 0;

    public PacketCollector(SimpleChannel channel) {
        this.channel = channel;
    }

    public <T> void registerClientboundPacket(ResourceLocation id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        channel.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(decoder)
                .encoder((packet, buf) -> ((com.stereowalker.unionlib.network.protocol.game.AbstractUnionPacket) packet).encode(buf))
                .consumerMainThread((packet, contextSupplier) -> ((com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket) packet).handle(contextSupplier.get()))
                .add();
    }

    public <T> void registerServerboundPacket(ResourceLocation id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        channel.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(decoder)
                .encoder((packet, buf) -> ((com.stereowalker.unionlib.network.protocol.game.AbstractUnionPacket) packet).encode(buf))
                .consumerMainThread((packet, contextSupplier) -> ((com.stereowalker.unionlib.network.protocol.game.ServerboundUnionPacket) packet).handle(contextSupplier.get()))
                .add();
    }
}
