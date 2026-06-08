package hunternif.mc.impl.atlas.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stereowalker.unionlib.resource.ReloadListener;
import com.stereowalker.unionlib.util.VersionHelper;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.core.scaning.TileHeightType;
import hunternif.mc.impl.atlas.resource.ResourceReloadListener;
import hunternif.mc.impl.atlas.util.Log;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Client-only config mapping tile IDs to texture sets and render metadata.
 * <p>Must be loaded after {@link TextureSetConfig}!</p>
 *
 * @author Hunternif
 */
public class TileTextureConfig implements ResourceReloadListener<TileTextureConfig.TileTextureConfigData>, ReloadListener {
    public static final ResourceLocation ID = AntiqueAtlas.id("tile_textures");
    private final TileTextureMap tileTextureMap;
    private final TextureSetMap textureSetMap;

    public TileTextureConfig(TileTextureMap biomeTextureMap, TextureSetMap textureSetMap) {
        this.tileTextureMap = biomeTextureMap;
        this.textureSetMap = textureSetMap;
    }

    @Override
    public CompletableFuture<TileTextureConfigData> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, ResourceLocation> textureMap = new HashMap<>();
            Map<ResourceLocation, TileMetadata> metadataMap = new HashMap<>();

            try {
                for (Entry<ResourceLocation, Resource> id : manager.listResources("atlas/tiles", (s) -> s.toString().endsWith(".json")).entrySet()) {
                    ResourceLocation tile_id = VersionHelper.toLoc(id.getKey().getNamespace(), id.getKey().getPath().replace("atlas/tiles/", "").replace(".json", ""));

                    try {
                        Resource resource = id.getValue();
                        try (InputStream stream = resource.open(); InputStreamReader reader = new InputStreamReader(stream)) {
                            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                            TileMetadata metadata = readMetadata(object);

                            int version = object.getAsJsonPrimitive("version").getAsInt();
                            if (version == 1) {
                                ResourceLocation texture_set = VersionHelper.toLoc(object.get("texture_set").getAsString());

                                textureMap.put(tile_id, texture_set);
                                metadataMap.put(tile_id, metadata);

                                for (TileHeightType layer : TileHeightType.values()) {
                                    ResourceLocation layerId = ResourceLocation.tryParse(tile_id + "_" + layer.getName());
                                    textureMap.put(layerId, texture_set);
                                    metadataMap.put(layerId, metadata);
                                }
                            } else if (version == 2) {
                                ResourceLocation default_entry = TileTextureMap.DEFAULT_TEXTURE;

                                try {
                                    default_entry = VersionHelper.toLoc(object.getAsJsonObject("texture_sets").get("default").getAsString());
                                } catch (Exception ignored) {
                                }

                                // insert the old-style texture set with the default one
                                textureMap.put(tile_id, default_entry);
                                metadataMap.put(tile_id, metadata);

                                for (TileHeightType layer : TileHeightType.values()) {
                                    ResourceLocation texture_set = default_entry;

                                    try {
                                        texture_set = VersionHelper.toLoc(object.getAsJsonObject("texture_sets").get(layer.getName()).getAsString());
                                    } catch (Exception ignored) {
                                    }

                                    ResourceLocation layerId = ResourceLocation.tryParse(tile_id + "_" + layer);
                                    textureMap.put(layerId, texture_set);
                                    metadataMap.put(layerId, metadata);
                                }
                            } else {
                                AntiqueAtlas.LOG.warn("The tile " + tile_id + " is in the wrong version! Skipping.");
                            }
                        }
                    } catch (Exception e) {
                        AntiqueAtlas.LOG.warn("Error reading tile mapping " + tile_id + "!", e);
                    }
                }
            } catch (Throwable e) {
                Log.warn(e, "Failed to read tile mappings!");
            }

            return new TileTextureConfigData(textureMap, metadataMap);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(TileTextureConfigData tileData, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            tileTextureMap.clear();
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : tileData.textureSets().entrySet()) {
                ResourceLocation tile_id = entry.getKey();
                ResourceLocation texture_set = entry.getValue();
                TextureSet set = textureSetMap.getByName(entry.getValue());

                if (set == null) {
                    AntiqueAtlas.LOG.error("Missing texture set `{}` for tile `{}`. Using default.", texture_set, tile_id);

                    set = tileTextureMap.getDefaultTexture();
                }

                tileTextureMap.setTexture(entry.getKey(), set);
                tileTextureMap.setMetadata(entry.getKey(), tileData.metadata().get(entry.getKey()));
                if (AntiqueAtlas.CONFIG.resourcePackLogging)
                    Log.info("Loaded tile %s with texture set %s", tile_id, set.name);
            }
        }, executor);
    }

    private TileMetadata readMetadata(JsonObject object) {
        JsonObject lod = object.getAsJsonObject("lod");
        if (lod == null) {
            return TileMetadata.DEFAULT;
        }

        ResourceLocation lodGroup = null;
        if (lod.has("group")) {
            lodGroup = VersionHelper.toLoc(lod.get("group").getAsString());
        }

        int lodPriority = lod.has("priority") ? lod.get("priority").getAsInt() : 0;
        int lodMinCount = lod.has("min_count") ? lod.get("min_count").getAsInt() : 1;
        if (lodGroup == null && lodPriority == 0 && lodMinCount == 1) {
            return TileMetadata.DEFAULT;
        }

        return new TileMetadata(lodGroup, lodPriority, Math.max(1, lodMinCount));
    }

    public record TileTextureConfigData(
            Map<ResourceLocation, ResourceLocation> textureSets,
            Map<ResourceLocation, TileMetadata> metadata
    ) {}
    
    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.singleton(TextureSetConfig.ID);
    }
}
