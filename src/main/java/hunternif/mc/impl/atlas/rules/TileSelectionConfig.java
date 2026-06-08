package hunternif.mc.impl.atlas.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stereowalker.unionlib.resource.ReloadListener;
import com.stereowalker.unionlib.util.VersionHelper;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.resource.ResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class TileSelectionConfig implements ResourceReloadListener<TileSelectionRules>, ReloadListener {
    public static final ResourceLocation ID = AntiqueAtlas.id("tile_selection");
    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;

    private final TileSelectionRules targetRules;

    public TileSelectionConfig(TileSelectionRules targetRules) {
        this.targetRules = targetRules;
    }

    @Override
    public CompletableFuture<TileSelectionRules> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            TileSelectionRules rules = new TileSelectionRules();

            for (Entry<ResourceLocation, Resource> entry : manager.listResources("atlas/tile_selection", path -> path.toString().endsWith(".json")).entrySet()) {
                ResourceLocation resourceId = VersionHelper.toLoc(
                        entry.getKey().getNamespace(),
                        entry.getKey().getPath().replace("atlas/tile_selection/", "").replace(".json", "")
                );

                try (InputStream stream = entry.getValue().open(); InputStreamReader reader = new InputStreamReader(stream)) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    int version = object.getAsJsonPrimitive("version").getAsInt();
                    if (version != VERSION_1 && version != VERSION_2) {
                        throw new IllegalArgumentException("Unsupported tile selection config version: " + version);
                    }

                    if (version == VERSION_1) {
                        readSourcePriorities(resourceId, object, rules);
                        readTilePriorities(object, rules);
                        readTilePrefixPriorities(object, rules);
                    } else {
                        readRules(resourceId, object, rules);
                    }
                } catch (Exception e) {
                    AntiqueAtlas.LOG.warn("Error reading tile selection config {}!", resourceId, e);
                }
            }

            return rules;
        }, executor);
    }

    private void readRules(ResourceLocation resourceId, JsonObject object, TileSelectionRules rules) {
        JsonArray jsonRules = object.getAsJsonArray("rules");
        if (jsonRules == null) {
            return;
        }

        for (JsonElement element : jsonRules) {
            if (!element.isJsonObject()) {
                AntiqueAtlas.LOG.warn("Ignoring non-object tile selection rule in {}", resourceId);
                continue;
            }
            try {
                rules.addRule(parseRule(element.getAsJsonObject()));
            } catch (Exception e) {
                AntiqueAtlas.LOG.warn("Ignoring invalid tile selection rule in {}", resourceId, e);
            }
        }
    }

    private TileSelectionRule parseRule(JsonObject object) {
        TileSelectionSource source = TileSelectionSource.fromConfigKey(object.getAsJsonPrimitive("source").getAsString());
        int priority = object.getAsJsonPrimitive("priority").getAsInt();

        JsonObject match = object.getAsJsonObject("match");
        String tile = readOptionalString(match, object, "tile");
        String tilePrefix = readOptionalString(match, object, "tile_prefix");
        ResourceLocation tileId = tile == null ? null : ResourceLocation.tryParse(tile);

        if (tile != null && tileId == null) {
            throw new IllegalArgumentException("Invalid tile id: " + tile);
        }

        return new TileSelectionRule(source, priority, tileId, tilePrefix, readDimensions(object), readOutputTiles(object));
    }

    private List<ResourceLocation> readOutputTiles(JsonObject object) {
        List<ResourceLocation> outputTiles = new ArrayList<>();
        readTileArray(object, "tiles", outputTiles);
        readTileArray(object, "output_tiles", outputTiles);
        readTileArray(object, "result_tiles", outputTiles);
        readSingleTile(object, "output_tile", outputTiles);
        readSingleTile(object, "result_tile", outputTiles);
        return outputTiles;
    }

    private void readTileArray(JsonObject object, String key, List<ResourceLocation> outputTiles) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            ResourceLocation tileId = ResourceLocation.tryParse(element.getAsString());
            if (tileId == null) {
                throw new IllegalArgumentException("Invalid output tile id: " + element.getAsString());
            }
            outputTiles.add(tileId);
        }
    }

    private void readSingleTile(JsonObject object, String key, List<ResourceLocation> outputTiles) {
        if (!object.has(key)) {
            return;
        }
        ResourceLocation tileId = ResourceLocation.tryParse(object.getAsJsonPrimitive(key).getAsString());
        if (tileId == null) {
            throw new IllegalArgumentException("Invalid output tile id: " + object.getAsJsonPrimitive(key).getAsString());
        }
        outputTiles.add(tileId);
    }

    private Set<ResourceLocation> readDimensions(JsonObject object) {
        Set<ResourceLocation> dimensions = new HashSet<>();
        if (object.has("dimension")) {
            ResourceLocation dimension = ResourceLocation.tryParse(object.getAsJsonPrimitive("dimension").getAsString());
            if (dimension != null) {
                dimensions.add(dimension);
            }
        }

        JsonArray dimensionsArray = object.getAsJsonArray("dimensions");
        if (dimensionsArray != null) {
            for (JsonElement dimensionElement : dimensionsArray) {
                ResourceLocation dimension = ResourceLocation.tryParse(dimensionElement.getAsString());
                if (dimension != null) {
                    dimensions.add(dimension);
                }
            }
        }

        return dimensions;
    }

    private String readOptionalString(JsonObject primary, JsonObject fallback, String key) {
        if (primary != null && primary.has(key)) {
            return primary.getAsJsonPrimitive(key).getAsString();
        }
        if (fallback.has(key)) {
            return fallback.getAsJsonPrimitive(key).getAsString();
        }
        return null;
    }

    private void readSourcePriorities(ResourceLocation resourceId, JsonObject object, TileSelectionRules rules) {
        JsonObject sourcePriorities = object.getAsJsonObject("source_priorities");
        if (sourcePriorities == null) {
            return;
        }

        for (Entry<String, JsonElement> entry : sourcePriorities.entrySet()) {
            try {
                TileSelectionSource source = TileSelectionSource.fromConfigKey(entry.getKey());
                rules.setSourcePriority(source, entry.getValue().getAsInt());
            } catch (Exception e) {
                AntiqueAtlas.LOG.warn("Ignoring invalid tile selection source {} in {}", entry.getKey(), resourceId);
            }
        }
    }

    private void readTilePriorities(JsonObject object, TileSelectionRules rules) {
        JsonObject tilePriorities = object.getAsJsonObject("tile_priorities");
        if (tilePriorities == null) {
            return;
        }

        for (Entry<String, JsonElement> entry : tilePriorities.entrySet()) {
            ResourceLocation tileId = ResourceLocation.tryParse(entry.getKey());
            if (tileId == null) {
                AntiqueAtlas.LOG.warn("Ignoring invalid tile id in tile selection config: {}", entry.getKey());
                continue;
            }
            rules.setTilePriority(tileId, entry.getValue().getAsInt());
        }
    }

    private void readTilePrefixPriorities(JsonObject object, TileSelectionRules rules) {
        JsonObject prefixPriorities = object.getAsJsonObject("tile_prefix_priorities");
        if (prefixPriorities == null) {
            return;
        }

        for (Entry<String, JsonElement> entry : prefixPriorities.entrySet()) {
            rules.addRule(new TileSelectionRule(TileSelectionSource.GLOBAL, entry.getValue().getAsInt(), null, entry.getKey(), Collections.emptySet(), Collections.emptyList()));
        }
    }

    @Override
    public CompletableFuture<Void> apply(TileSelectionRules data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> targetRules.replaceWith(data), executor);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }
}
