package hunternif.mc.api;

import java.util.Collections;
import java.util.List;

import com.stereowalker.unionlib.util.RegistryHelper;
import com.stereowalker.unionlib.util.VersionHelper;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.api.impl.MarkerApiImpl;
import hunternif.mc.impl.atlas.api.impl.TileApiImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Use this class to obtain a reference to the APIs.
 *
 * @author Hunternif
 */
public class AtlasAPI {
    private static final int VERSION = 5;
    private static final TileAPI tiles = new TileApiImpl();
    private static final MarkerAPI markers = new MarkerApiImpl();

    /**
     * Version of the API, meaning only this particular class. You might
     * want to check static field VERSION in the specific API interfaces.
     */
    public static int getVersion() {
        return VERSION;
    }

    public static Item getAtlasItem() {
        return RegistryHelper.items().get(VersionHelper.toLoc("antiqueatlas:antique_atlas"));
    }

    /**
     * API for biomes and custom tiles (i.e. dungeons, towns etc).
     */
    public static TileAPI getTileAPI() {
        return tiles;
    }

    /**
     * API for custom markers.
     */
    public static MarkerAPI getMarkerAPI() {
        return markers;
    }

    /**
     * Convenience method that returns a list of atlas IDs that are currently
     * active for the player, including the built-in atlas when enabled.
     **/
    public static List<Integer> getPlayerAtlases(Player player) {
        List<Integer> atlasIds = AntiqueAtlas.activeAtlasResolver.getActiveAtlasIds(player);
        return atlasIds.isEmpty() ? Collections.emptyList() : atlasIds;
    }
}
