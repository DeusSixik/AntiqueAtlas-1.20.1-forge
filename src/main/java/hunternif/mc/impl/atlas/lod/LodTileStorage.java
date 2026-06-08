package hunternif.mc.impl.atlas.lod;

import hunternif.mc.impl.atlas.client.TileTextureMap;
import hunternif.mc.impl.atlas.core.ITileStorage;
import hunternif.mc.impl.atlas.rules.TileSelectionRules;
import hunternif.mc.impl.atlas.util.Rect;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class LodTileStorage implements ITileStorage {
    private final ITileStorage source;
    private final int lodStep;
    private final Rect scope;
    private final ResourceLocation dimensionId;
    private final TileSelectionRules rules;
    private final Map<Long, ResourceLocation> cache = new HashMap<>();

    public LodTileStorage(ITileStorage source, Rect requestedScope, int lodStep, ResourceLocation dimensionId, TileSelectionRules rules) {
        this.source = source;
        this.lodStep = lodStep;
        this.dimensionId = dimensionId;
        this.rules = rules;
        this.scope = new Rect(
                floorMultiple(requestedScope.minX, lodStep),
                floorMultiple(requestedScope.minY, lodStep),
                ceilMultiple(requestedScope.maxX, lodStep),
                ceilMultiple(requestedScope.maxY, lodStep)
        );
    }

    @Override
    public void setTile(int x, int y, ResourceLocation tile) {
        throw new UnsupportedOperationException("LOD storage is read-only");
    }

    @Override
    public ResourceLocation removeTile(int x, int y) {
        throw new UnsupportedOperationException("LOD storage is read-only");
    }

    @Override
    public ResourceLocation getTile(int x, int y) {
        if (lodStep <= 1) {
            return source.getTile(x, y);
        }
        if (x < scope.minX || x > scope.maxX || y < scope.minY || y > scope.maxY) {
            return null;
        }

        int alignedX = floorMultiple(x, lodStep);
        int alignedY = floorMultiple(y, lodStep);
        long key = pack(alignedX, alignedY);
        ResourceLocation cached = cache.get(key);
        if (cached != null || cache.containsKey(key)) {
            return cached;
        }

        ResourceLocation resolved = aggregateTile(alignedX, alignedY);
        cache.put(key, resolved);
        return resolved;
    }

    @Override
    public boolean hasTileAt(int x, int y) {
        return getTile(x, y) != null;
    }

    @Override
    public Rect getScope() {
        return scope;
    }

    private ResourceLocation aggregateTile(int startX, int startY) {
        Map<ResourceLocation, Integer> frequencies = new HashMap<>();
        Map<ResourceLocation, GroupCandidate> structureGroups = new HashMap<>();
        int total = lodStep * lodStep;
        int emptyCount = 0;

        for (int dx = 0; dx < lodStep; dx++) {
            for (int dy = 0; dy < lodStep; dy++) {
                ResourceLocation tile = source.getTile(startX + dx, startY + dy);
                if (tile == null) {
                    emptyCount++;
                    continue;
                }

                frequencies.merge(tile, 1, Integer::sum);
            }
        }

        for (Map.Entry<ResourceLocation, Integer> entry : frequencies.entrySet()) {
            ResourceLocation tile = entry.getKey();
            int frequency = entry.getValue();
            if (frequency < TileTextureMap.instance().getLodMinCount(tile)) {
                continue;
            }

            int structurePriority = rules.getLodStructurePriority(tile, dimensionId);
            if (structurePriority > Integer.MIN_VALUE) {
                ResourceLocation groupId = TileTextureMap.instance().getLodGroup(tile);
                if (groupId == null) {
                    groupId = tile;
                }

                GroupCandidate group = structureGroups.get(groupId);
                if (group == null) {
                    group = new GroupCandidate(groupId, structurePriority);
                    structureGroups.put(groupId, group);
                }
                group.includeTile(tile, structurePriority, frequency, rules.getLodTieBreakerPriority(tile, dimensionId));
            }
        }

        GroupCandidate bestStructureGroup = null;
        for (GroupCandidate group : structureGroups.values()) {
            if (bestStructureGroup == null || group.isBetterThan(bestStructureGroup)) {
                bestStructureGroup = group;
            }
        }
        if (bestStructureGroup != null) {
            return bestStructureGroup.tile;
        }
        if (emptyCount * 2 >= total) {
            return null;
        }

        ResourceLocation bestTile = null;
        int bestCount = -1;
        int bestTieBreaker = Integer.MIN_VALUE;
        for (Map.Entry<ResourceLocation, Integer> entry : frequencies.entrySet()) {
            if (entry.getValue() < TileTextureMap.instance().getLodMinCount(entry.getKey())) {
                continue;
            }
            int count = entry.getValue();
            int tieBreaker = rules.getLodTieBreakerPriority(entry.getKey(), dimensionId);
            if (count > bestCount || (count == bestCount && tieBreaker > bestTieBreaker)) {
                bestTile = entry.getKey();
                bestCount = count;
                bestTieBreaker = tieBreaker;
            }
        }

        return bestTile;
    }

    private static long pack(int x, int y) {
        return ((long) x << 32) ^ (y & 0xffffffffL);
    }

    private static int floorMultiple(int value, int step) {
        return Math.floorDiv(value, step) * step;
    }

    private static int ceilMultiple(int value, int step) {
        return (-Math.floorDiv(-value, step)) * step;
    }

    private static final class GroupCandidate {
        private final ResourceLocation groupId;
        private int groupStructurePriority;
        private int totalCount;
        private ResourceLocation tile;
        private int tileStructurePriority = Integer.MIN_VALUE;
        private int tileLodPriority = Integer.MIN_VALUE;
        private int tileCount = -1;
        private int tieBreaker = Integer.MIN_VALUE;

        private GroupCandidate(ResourceLocation groupId, int structurePriority) {
            this.groupId = groupId;
            this.groupStructurePriority = structurePriority;
        }

        private void includeTile(ResourceLocation tile, int structurePriority, int frequency, int tieBreaker) {
            this.totalCount += frequency;
            if (structurePriority > this.groupStructurePriority) {
                this.groupStructurePriority = structurePriority;
            }

            int lodPriority = TileTextureMap.instance().getLodPriority(tile);
            if (this.tile == null
                    || structurePriority > tileStructurePriority
                    || lodPriority > tileLodPriority
                    || (lodPriority == tileLodPriority && frequency > tileCount)
                    || (lodPriority == tileLodPriority && frequency == tileCount && tieBreaker > this.tieBreaker)) {
                this.tile = tile;
                this.tileStructurePriority = structurePriority;
                this.tileLodPriority = lodPriority;
                this.tileCount = frequency;
                this.tieBreaker = tieBreaker;
            }
        }

        private boolean isBetterThan(GroupCandidate other) {
            if (groupStructurePriority != other.groupStructurePriority) {
                return groupStructurePriority > other.groupStructurePriority;
            }
            if (totalCount != other.totalCount) {
                return totalCount > other.totalCount;
            }
            if (tileStructurePriority != other.tileStructurePriority) {
                return tileStructurePriority > other.tileStructurePriority;
            }
            if (tileLodPriority != other.tileLodPriority) {
                return tileLodPriority > other.tileLodPriority;
            }
            if (tileCount != other.tileCount) {
                return tileCount > other.tileCount;
            }
            if (tieBreaker != other.tieBreaker) {
                return tieBreaker > other.tieBreaker;
            }
            return groupId.toString().compareTo(other.groupId.toString()) < 0;
        }
    }
}
