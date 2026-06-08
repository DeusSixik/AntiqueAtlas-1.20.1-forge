package com.stereowalker.unionlib.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.simple.SimpleChannel;

public abstract class AbstractUnionPacket {
    protected final SimpleChannel channel;

    protected AbstractUnionPacket(SimpleChannel channel) {
        this.channel = channel;
    }

    protected AbstractUnionPacket(FriendlyByteBuf ignored, SimpleChannel channel) {
        this.channel = channel;
    }

    public abstract void encode(FriendlyByteBuf buffer);

    public abstract ResourceLocation id();
}
