package hunternif.mc.impl.atlas.network.packet.c2s.play;

import com.stereowalker.unionlib.network.protocol.game.ServerboundUnionPacket;
import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.util.Log;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class UpdateMarkerC2SPacket extends ServerboundUnionPacket {
    public static final ResourceLocation ID = AntiqueAtlas.id("packet", "c2s", "marker", "update");

    int atlasID;
    int markerID;
    ResourceLocation markerType;
    Component label;

    public UpdateMarkerC2SPacket(int atlasID, int markerID, ResourceLocation markerType, Component label) {
        super(AntiqueAtlas.instance.channel);
        this.atlasID = atlasID;
        this.markerID = markerID;
        this.markerType = markerType;
        this.label = label;
    }

    public UpdateMarkerC2SPacket(FriendlyByteBuf packetBuffer) {
        super(packetBuffer, AntiqueAtlas.instance.channel);
        this.atlasID = packetBuffer.readVarInt();
        this.markerID = packetBuffer.readVarInt();
        this.markerType = packetBuffer.readResourceLocation();
        this.label = Component.Serializer.fromJson(packetBuffer.readUtf());
    }

    @Override
    public void encode(FriendlyByteBuf packetBuffer) {
        packetBuffer.writeVarInt(atlasID);
        packetBuffer.writeVarInt(markerID);
        packetBuffer.writeResourceLocation(markerType);
        packetBuffer.writeUtf(Component.Serializer.toJson(label));
    }

    @Override
    public boolean handleOnServer(ServerPlayer sender) {
        if (!AtlasAPI.getPlayerAtlases(sender).contains(atlasID)) {
            Log.warn("Player %s attempted to update marker in someone else's Atlas #%d",
                    sender.getName(), atlasID);
            return true;
        }

        AtlasAPI.getMarkerAPI().updateMarker(sender.level(), atlasID, markerID, markerType, label);
        return true;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
