package com.stereowalker.unionlib.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public abstract class ClientboundUnionPacket extends AbstractUnionPacket {
    protected ClientboundUnionPacket(SimpleChannel channel) {
        super(channel);
    }

    protected ClientboundUnionPacket(FriendlyByteBuf buffer, SimpleChannel channel) {
        super(buffer, channel);
    }

    public abstract boolean runOnClient(Player sender);

    public void handle(NetworkEvent.Context context) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        runOnClient(player);
        context.setPacketHandled(true);
    }

    public void send(ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), this);
    }

    public void send(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            send(player);
        }
    }

    public void send(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player);
        }
    }
}
