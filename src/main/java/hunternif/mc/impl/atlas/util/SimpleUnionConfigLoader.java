package hunternif.mc.impl.atlas.util;

import com.google.common.base.CaseFormat;
import com.google.gson.*;
import com.stereowalker.unionlib.config.UnionConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SimpleUnionConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SimpleUnionConfigLoader() {
    }

    public static void load(String modId, Object config) {
        if (config == null) {
            return;
        }

        Path configDir = FMLPaths.CONFIGDIR.get();
        Path path = configDir.resolve(modId + "-client.json");
        JsonObject root = readRoot(path);

        boolean changed = false;
        for (Field field : config.getClass().getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String key = resolveKey(field);
            JsonElement element = root.get(key);
            try {
                if (element != null) {
                    Object value = GSON.fromJson(element, field.getGenericType());
                    if (value != null) {
                        field.set(config, value);
                        continue;
                    }
                }

                root.add(key, GSON.toJsonTree(field.get(config)));
                changed = true;
            } catch (Exception e) {
                Log.warn(e, "Failed to load config field %s", field.getName());
            }
        }

        if (changed || !Files.exists(path)) {
            writeRoot(path, root);
        }
    }

    private static JsonObject readRoot(Path path) {
        if (!Files.exists(path)) {
            return new JsonObject();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            Log.warn(e, "Failed to read config %s", path);
            return new JsonObject();
        }
    }

    private static void writeRoot(Path path, JsonObject root) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Log.warn(e, "Failed to write config %s", path);
        }
    }

    private static String resolveKey(Field field) {
        UnionConfig.Entry entry = field.getAnnotation(UnionConfig.Entry.class);
        if (entry != null && !entry.name().isEmpty()) {
            return entry.name();
        }

        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
    }
}
