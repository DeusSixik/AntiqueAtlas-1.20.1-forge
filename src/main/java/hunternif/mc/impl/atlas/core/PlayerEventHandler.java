package hunternif.mc.impl.atlas.core;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.identity.AtlasIdentityService;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.network.packet.s2c.play.SyncPlayerAtlasIdS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PlayerEventHandler {
    public static void onPlayerLogin(ServerPlayer player) {
        if (!AtlasIdentityService.isPlayerAtlasEnabled()) {
            return;
        }

        Level world = player.level();
        int atlasID = AtlasIdentityService.getOrCreatePlayerAtlasId(player);

        new SyncPlayerAtlasIdS2CPacket(atlasID).send(player);

        AtlasData data = AntiqueAtlas.tileData.getData(atlasID, world);
        // On the player join send the map from the server to the client:
        if (!data.isEmpty()) {
            data.syncToPlayer(atlasID, player);
        }

        // Same thing with the local markers:
        MarkersData markers = AntiqueAtlas.markersData.getMarkersData(atlasID, world);
        if (!markers.isEmpty()) {
            markers.syncToPlayer(atlasID, player);
        }
    }

    public static void onPlayerTick(Player player) {
        if (AtlasIdentityService.isPlayerAtlasEnabled()) {
            // TODO Can we move world scanning to the server in this case as well?
            AtlasData data = AntiqueAtlas.tileData.getData(
                    AtlasIdentityService.getOrCreatePlayerAtlasId(player), player.level());

            AntiqueAtlas.worldScanner.updateAtlasAroundPlayer(data, player);
        }
    }
}
