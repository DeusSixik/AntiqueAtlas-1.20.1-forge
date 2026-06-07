package hunternif.mc.impl.atlas.rules;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.ResourceLocation;

public class TileSelectionRules {
    private static final int LEGACY_STRUCTURE_PRIORITY_BASE = 1000;

    private final EnumMap<TileSelectionSource, Integer> sourcePriorities = new EnumMap<>(TileSelectionSource.class);
    private final Map<ResourceLocation, Integer> tilePriorities = new HashMap<>();
    private final List<TileSelectionRule> rules = new CopyOnWriteArrayList<>();

    public TileSelectionRules() {
        resetToDefaults();
    }

    public synchronized void resetToDefaults() {
        sourcePriorities.clear();
        sourcePriorities.put(TileSelectionSource.GLOBAL, 1000);
        sourcePriorities.put(TileSelectionSource.BIOME, 0);
        tilePriorities.clear();
        rules.clear();
    }

    public synchronized void setSourcePriority(TileSelectionSource source, int priority) {
        sourcePriorities.put(source, priority);
    }

    public synchronized void setTilePriority(ResourceLocation tileId, int priority) {
        tilePriorities.put(tileId, priority);
    }

    public synchronized void addRule(TileSelectionRule rule) {
        rules.add(rule);
    }

    public synchronized int getPriority(TileSelectionSource source, ResourceLocation tileId, ResourceLocation dimensionId) {
        if (tileId == null) {
            return Integer.MIN_VALUE;
        }

        Integer exactPriority = tilePriorities.get(tileId);
        if (exactPriority != null) {
            return exactPriority;
        }

        int bestPriority = sourcePriorities.getOrDefault(source, 0);
        for (TileSelectionRule rule : rules) {
            if (rule.matches(source, tileId, dimensionId) && rule.priority() > bestPriority) {
                bestPriority = rule.priority();
            }
        }
        return bestPriority;
    }

    public synchronized int getExplicitPriority(TileSelectionSource source, ResourceLocation tileId, ResourceLocation dimensionId) {
        if (tileId == null) {
            return Integer.MIN_VALUE;
        }

        Integer exactPriority = tilePriorities.get(tileId);
        int bestPriority = exactPriority != null ? exactPriority : Integer.MIN_VALUE;
        for (TileSelectionRule rule : rules) {
            if (rule.isExplicitMatcher() && rule.matches(source, tileId, dimensionId) && rule.priority() > bestPriority) {
                bestPriority = rule.priority();
            }
        }
        return bestPriority;
    }

    public synchronized int getStructurePriority(ResourceLocation tileId, ResourceLocation dimensionId, int fallbackPriority) {
        if (tileId == null) {
            return Integer.MIN_VALUE;
        }

        return Math.max(getPriority(TileSelectionSource.GLOBAL, tileId, dimensionId), LEGACY_STRUCTURE_PRIORITY_BASE - fallbackPriority);
    }

    public synchronized int getLodStructurePriority(ResourceLocation tileId, ResourceLocation dimensionId) {
        return getExplicitPriority(TileSelectionSource.GLOBAL, tileId, dimensionId);
    }

    public synchronized int getLodTieBreakerPriority(ResourceLocation tileId, ResourceLocation dimensionId) {
        int globalPriority = getExplicitPriority(TileSelectionSource.GLOBAL, tileId, dimensionId);
        int biomePriority = getExplicitPriority(TileSelectionSource.BIOME, tileId, dimensionId);
        return Math.max(globalPriority, biomePriority);
    }

    public synchronized void replaceWith(TileSelectionRules other) {
        sourcePriorities.clear();
        sourcePriorities.putAll(other.sourcePriorities);
        tilePriorities.clear();
        tilePriorities.putAll(other.tilePriorities);
        rules.clear();
        rules.addAll(other.rules);
    }

    public synchronized Map<ResourceLocation, Integer> getTilePrioritiesView() {
        return Collections.unmodifiableMap(new HashMap<>(tilePriorities));
    }
}
