package hunternif.mc.impl.atlas.core;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.identity.AtlasIdentityService;
import hunternif.mc.impl.atlas.network.packet.s2c.play.SyncPlayerAtlasIdS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerEventHandler {
    public static void onPlayerLogin(ServerPlayer player) {
        if (AtlasIdentityService.isPlayerAtlasEnabled()) {
            int atlasID = AtlasIdentityService.getOrCreatePlayerAtlasId(player);
            String atlasName = AtlasIdentityService.getAtlasName(player.level(), atlasID).orElse(null);
            new SyncPlayerAtlasIdS2CPacket(atlasID, atlasName).send(player);
        }

        AntiqueAtlas.atlasScanService.syncActiveAtlasesToPlayer(player);
    }

    public static void onPlayerTick(Player player) {
        AntiqueAtlas.atlasScanService.updateActiveAtlasesAroundPlayer(player);
    }
}
