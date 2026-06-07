package hunternif.mc.impl.atlas.service;

import java.util.Collection;
import java.util.Objects;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.AtlasData;
import hunternif.mc.impl.atlas.core.TileInfo;
import hunternif.mc.impl.atlas.identity.AtlasReference;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.network.packet.s2c.play.DimensionUpdateS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class AtlasScanService {
    private final ActiveAtlasResolver activeAtlasResolver;

    public AtlasScanService(ActiveAtlasResolver activeAtlasResolver) {
        this.activeAtlasResolver = activeAtlasResolver;
    }

    public void syncAtlasToPlayer(ServerPlayer player, int atlasId) {
        AtlasData data = AntiqueAtlas.tileData.getData(atlasId, player.level());
        if (!data.isSyncedToPlayer(player) && !data.isEmpty()) {
            data.syncToPlayer(atlasId, player);
        }

        MarkersData markers = AntiqueAtlas.markersData.getMarkersData(atlasId, player.level());
        if (!markers.isSyncedOnPlayer(player) && !markers.isEmpty()) {
            markers.syncToPlayer(atlasId, player);
        }
    }

    public void syncActiveAtlasesToPlayer(ServerPlayer player) {
        for (AtlasReference atlas : activeAtlasResolver.getActiveAtlases(player)) {
            syncAtlasToPlayer(player, atlas.atlasId());
        }
    }

    public Collection<TileInfo> updateAtlasAroundPlayer(Player player, int atlasId) {
        AtlasData data = AntiqueAtlas.tileData.getData(atlasId, player.level());
        return AntiqueAtlas.worldScanner.updateAtlasAroundPlayer(data, player);
    }

    public void updateActiveAtlasesAroundPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        for (AtlasReference atlas : activeAtlasResolver.getActiveAtlases(player)) {
            Collection<TileInfo> newTiles = updateAtlasAroundPlayer(player, atlas.atlasId());
            if (!newTiles.isEmpty()) {
                new DimensionUpdateS2CPacket(atlas.atlasId(), player.level().dimension(), newTiles)
                        .send(Objects.requireNonNull(player.getServer()));
            }
        }
    }
}
