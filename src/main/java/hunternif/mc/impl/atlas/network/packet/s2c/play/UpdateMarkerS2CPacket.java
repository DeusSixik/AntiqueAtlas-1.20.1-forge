package hunternif.mc.impl.atlas.network.packet.s2c.play;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkersData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class UpdateMarkerS2CPacket extends ClientboundUnionPacket {
    public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "marker", "update");

    int atlasID;
    ResourceKey<Level> world;
    ResourceLocation markerType;
    Marker.Precursor marker;

    public UpdateMarkerS2CPacket(int atlasID, Marker marker) {
        super(AntiqueAtlas.instance.channel);
        this.atlasID = atlasID;
        this.world = marker.getWorld();
        this.markerType = marker.getType();
        this.marker = new Marker.Precursor(marker);
    }

    public UpdateMarkerS2CPacket(FriendlyByteBuf byteBuf) {
        super(byteBuf, AntiqueAtlas.instance.channel);
        this.atlasID = byteBuf.readVarInt();
        this.world = ResourceKey.create(Registries.DIMENSION, byteBuf.readResourceLocation());
        this.markerType = byteBuf.readResourceLocation();
        this.marker = new Marker.Precursor(byteBuf);
    }

    @Override
    public void encode(FriendlyByteBuf byteBuf) {
        byteBuf.writeVarInt(atlasID);
        byteBuf.writeResourceLocation(world.location());
        byteBuf.writeResourceLocation(markerType);
        new Marker(markerType, world, marker).write(byteBuf);
    }

    @Override
    public boolean runOnClient(Player sender) {
        MarkersData markersData = AntiqueAtlas.markersData.getMarkersDataCached(atlasID, world);
        markersData.updateMarker(marker.getId(), markerType, marker.getLabel());
        AntiqueAtlasClientSegment.getAtlasGUI().updateBookmarkerList();
        return true;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
