package hunternif.mc.impl.atlas.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.network.packet.s2c.play.DeleteMarkerS2CPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class DeathMarkerService {
    public static final int DEFAULT_DEATH_MARKER_LIMIT = 5;
    private static final ResourceLocation TOMB_MARKER_ID = AntiqueAtlas.id("tomb");

    public void trimExcessDeathMarkers(Level world, int atlasId) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }

        MarkersData data = AntiqueAtlas.markersData.getMarkersData(atlasId, world);
        List<Marker> tombMarkers = collectDeathMarkers(data);
        int limit = Math.max(0, AntiqueAtlas.CONFIG.deathMarkerLimit);

        if (tombMarkers.size() <= limit) {
            return;
        }

        tombMarkers.sort(Comparator.comparingInt(Marker::getId));
        int excess = tombMarkers.size() - limit;
        for (int i = 0; i < excess; i++) {
            Marker marker = tombMarkers.get(i);
            if (data.removeMarker(marker.getId()) != null) {
                new DeleteMarkerS2CPacket(atlasId, marker.getId()).send(serverLevel.getServer());
            }
        }
    }

    private List<Marker> collectDeathMarkers(MarkersData data) {
        List<Marker> tombMarkers = new ArrayList<>();
        for (ResourceKey<Level> dimension : data.getVisitedDimensions()) {
            for (Marker marker : data.getMarkersInWorld(dimension)) {
                if (TOMB_MARKER_ID.equals(marker.getType())) {
                    tombMarkers.add(marker);
                }
            }
        }
        return tombMarkers;
    }
}
