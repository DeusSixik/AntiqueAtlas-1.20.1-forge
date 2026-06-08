package hunternif.mc.impl.atlas.structure;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class StructurePieceConfig implements ResourceReloadListener<List<StructurePieceConfig.EntryData>>, ReloadListener {
    private static final ResourceLocation ID = AntiqueAtlas.id("structure_piece_tiles");
    private static final int VERSION = 1;

    @Override
    public CompletableFuture<List<EntryData>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            List<EntryData> entries = new ArrayList<>();

            for (Entry<ResourceLocation, Resource> resourceEntry : manager.listResources("atlas/structure_pieces", path -> path.toString().endsWith(".json")).entrySet()) {
                ResourceLocation resourceId = VersionHelper.toLoc(
                        resourceEntry.getKey().getNamespace(),
                        resourceEntry.getKey().getPath().replace("atlas/structure_pieces/", "").replace(".json", "")
                );

                try (InputStream stream = resourceEntry.getValue().open(); InputStreamReader reader = new InputStreamReader(stream)) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    int version = object.getAsJsonPrimitive("version").getAsInt();
                    if (version != VERSION) {
                        throw new IllegalArgumentException("Unsupported structure piece config version: " + version);
                    }

                    ResourceLocation pieceTypeId = VersionHelper.toLoc(object.getAsJsonPrimitive("piece_type").getAsString());
                    List<ResourceLocation> tileIds = readTiles(object, resourceId, "tile", "tiles");
                    int priority = object.getAsJsonPrimitive("priority").getAsInt();
                    StructureHandler.Setter setter = readSetter(object, resourceId);
                    entries.add(new EntryData(pieceTypeId, tileIds, priority, setter));
                } catch (Exception e) {
                    AntiqueAtlas.LOG.warn("Error reading structure piece config {}!", resourceId, e);
                }
            }

            return entries;
        }, executor);
    }

    private StructureHandler.Setter readSetter(JsonObject object, ResourceLocation resourceId) {
        if (!object.has("setter")) {
            return StructureHandler.ALWAYS;
        }

        return StructureHandler.setterByName(object.getAsJsonPrimitive("setter").getAsString(), resourceId);
    }

    private List<ResourceLocation> readTiles(JsonObject object, ResourceLocation resourceId, String singleKey, String multiKey) {
        List<ResourceLocation> tiles = new ArrayList<>();
        JsonArray array = object.getAsJsonArray(multiKey);
        if (array != null) {
            for (JsonElement element : array) {
                ResourceLocation tileId = VersionHelper.toLoc(element.getAsString());
                if (tileId == null) {
                    throw new IllegalArgumentException("Invalid tile id in `" + multiKey + "`: " + element);
                }
                tiles.add(tileId);
            }
        } else if (object.has(singleKey)) {
            ResourceLocation tileId = VersionHelper.toLoc(object.getAsJsonPrimitive(singleKey).getAsString());
            if (tileId != null) {
                tiles.add(tileId);
            }
        }

        if (tiles.isEmpty()) {
            throw new IllegalArgumentException("Missing `" + singleKey + "` or non-empty `" + multiKey + "` in " + resourceId);
        }

        return List.copyOf(tiles);
    }

    @Override
    public CompletableFuture<Void> apply(List<EntryData> entries, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            StructureHandler.clearStructurePieceTileRegistrations();
            for (EntryData entry : entries) {
                StructureHandler.registerTile(entry.pieceTypeId(), entry.priority(), entry.tileIds(), entry.setter());
            }
        }, executor);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }

    public record EntryData(
            ResourceLocation pieceTypeId,
            List<ResourceLocation> tileIds,
            int priority,
            StructureHandler.Setter setter
    ) {}
}
