package com.stereowalker.unionlib.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

public abstract class ServerboundUnionPacket extends AbstractUnionPacket {
    protected ServerboundUnionPacket(SimpleChannel channel) {
        super(channel);
    }

    protected ServerboundUnionPacket(FriendlyByteBuf buffer, SimpleChannel channel) {
        super(buffer, channel);
    }

    public abstract boolean handleOnServer(ServerPlayer sender);

    public void handle(NetworkEvent.Context context) {
        handleOnServer(context.getSender());
        context.setPacketHandled(true);
    }

    public void send() {
        channel.sendToServer(this);
    }
}
