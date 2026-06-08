package hunternif.mc.impl.atlas.structure;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.util.MathUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StructureHandler {
    private static final HashMultimap<ResourceLocation, RegisteredTile> STRUCTURE_PIECE_TO_TILE_MAP = HashMultimap.create();
    private static final Multimap<ResourceLocation, RegisteredTile> JIGSAW_TO_TILE_MAP = HashMultimap.create();
    private static final Map<ResourceLocation, Integer> STRUCTURE_PIECE_TILE_PRIORITY = new HashMap<>();
    private static final Set<ResourceLocation> LOGGED_MISSING_STRUCTURE_PIECES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<ResourceLocation> LOGGED_MISSING_JIGSAW_PATTERNS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static final Setter ALWAYS = (world, element, box, rotation) -> Collections.singleton(new ChunkPos(MathUtil.getCenter(box).getX() >> 4, MathUtil.getCenter(box).getZ() >> 4));


    public static Collection<ChunkPos> IF_X_DIRECTION(Level ignoredWorld, StructurePoolElement ignoredElement, BoundingBox box, StructurePiece piece) {
        if (piece instanceof PoolElementStructurePiece poolPiece) {
            List<JigsawJunction> junctions = poolPiece.getJunctions();
            if (junctions.size() == 2) {
                if (junctions.get(0).getSourceX() == junctions.get(1).getSourceX() || junctions.get(0).getSourceZ() != junctions.get(1).getSourceZ()) {
                    return Collections.singleton(new ChunkPos(MathUtil.getCenter(box)));
                }
            } else {
                return Collections.singleton(new ChunkPos(MathUtil.getCenter(box)));
            }
        }
        return Collections.emptyList();
    }

    public static Collection<ChunkPos> IF_Z_DIRECTION(Level ignoredWorld, StructurePoolElement ignoredElement, BoundingBox box, StructurePiece piece) {
        if (piece instanceof PoolElementStructurePiece poolPiece) {
            List<JigsawJunction> junctions = poolPiece.getJunctions();
            if (junctions.size() == 2) {
                if (junctions.get(0).getSourceZ() == junctions.get(1).getSourceZ() || junctions.get(0).getSourceX() != junctions.get(1).getSourceX()) {
                    return Collections.singleton(new ChunkPos(MathUtil.getCenter(box)));
                }
            } else {
                return Collections.singleton(new ChunkPos(MathUtil.getCenter(box)));
            }
        }
        return Collections.emptyList();
    }

    public static Collection<ChunkPos> SPAN_X(Level ignoredWorld, StructurePoolElement ignoredElement, BoundingBox box, StructurePiece piece) {
        if (box.getXSpan() <= 16) {
            return Collections.emptyList();
        }

        Set<ChunkPos> matches = new java.util.HashSet<>();
        int chunkZ = MathUtil.getCenter(box).getZ() >> 4;
        for (int x = box.minX(); x < box.maxX(); x += 16) {
            matches.add(new ChunkPos(x >> 4, chunkZ));
        }
        return matches;
    }

    public static Collection<ChunkPos> SPAN_Z(Level ignoredWorld, StructurePoolElement ignoredElement, BoundingBox box, StructurePiece piece) {
        if (box.getZSpan() <= 16) {
            return Collections.emptyList();
        }

        Set<ChunkPos> matches = new java.util.HashSet<>();
        int chunkX = MathUtil.getCenter(box).getX() >> 4;
        for (int z = box.minZ(); z < box.maxZ(); z += 16) {
            matches.add(new ChunkPos(chunkX, z >> 4));
        }
        return matches;
    }

    public static Collection<ChunkPos> BRIDGE_END_X(Level ignoredWorld, StructurePoolElement ignoredElement, BoundingBox box, StructurePiece piece) {
        if (box.getXSpan() > box.getZSpan()) {
            return Collections.singleton(new ChunkPos(box.getCenter().getX() >> 4, box.getCenter().getZ() >> 4));
        }
        return Collections.emptySet();
    }

    public static Collection<ChunkPos> BRIDGE_END_Z(Level ignoredWorld, StructurePoolElement ignoredElement, BoundingBox box, StructurePiece piece) {
        if (box.getZSpan() > box.getXSpan()) {
            return Collections.singleton(new ChunkPos(box.getCenter().getX() >> 4, box.getCenter().getZ() >> 4));
        }
        return Collections.emptySet();
    }

    public static Collection<ChunkPos> ABOVE_GROUND(Level world, StructurePoolElement ignoredElement, BoundingBox box, StructurePiece piece) {
        if (world.getSeaLevel() - 4 <= box.getCenter().getY()) {
            return Collections.singleton(new ChunkPos(box.getCenter()));
        }
        return Collections.emptyList();
    }

    public static void clearStructurePieceTileRegistrations() {
        STRUCTURE_PIECE_TO_TILE_MAP.clear();
        STRUCTURE_PIECE_TILE_PRIORITY.clear();
        LOGGED_MISSING_STRUCTURE_PIECES.clear();
    }

    public static void clearJigsawTileRegistrations() {
        JIGSAW_TO_TILE_MAP.clear();
        LOGGED_MISSING_JIGSAW_PATTERNS.clear();
    }

    public static void registerTile(ResourceLocation structurePieceId, int priority, List<ResourceLocation> textureIds, Setter setter) {
        STRUCTURE_PIECE_TO_TILE_MAP.put(structurePieceId, new RegisteredTile(List.copyOf(textureIds), setter));
        for (ResourceLocation textureId : textureIds) {
            STRUCTURE_PIECE_TILE_PRIORITY.put(textureId, priority);
        }
    }

    public static void registerTile(ResourceLocation structurePieceId, int priority, ResourceLocation textureId, Setter setter) {
        registerTile(structurePieceId, priority, Collections.singletonList(textureId), setter);
    }

    public static void registerTile(ResourceLocation structurePieceId, int priority, ResourceLocation textureId) {
        registerTile(structurePieceId, priority, textureId, ALWAYS);
    }

    public static void registerTile(StructurePieceType structurePieceType, int priority, List<ResourceLocation> textureIds, Setter setter) {
        ResourceLocation id = BuiltInRegistries.STRUCTURE_PIECE.getKey(structurePieceType);
        registerTile(id, priority, textureIds, setter);
    }

    public static void registerTile(StructurePieceType structurePieceType, int priority, ResourceLocation textureId, Setter setter) {
        registerTile(structurePieceType, priority, Collections.singletonList(textureId), setter);
    }

    public static void registerTile(StructurePieceType structurePieceType, int priority, ResourceLocation textureId) {
        registerTile(structurePieceType, priority, textureId, ALWAYS);
    }

    public static void registerJigsawTile(ResourceLocation jigsawPattern, int priority, List<ResourceLocation> tileIds, Setter setter) {
        JIGSAW_TO_TILE_MAP.put(jigsawPattern, new RegisteredTile(List.copyOf(tileIds), setter));
        for (ResourceLocation tileId : tileIds) {
            STRUCTURE_PIECE_TILE_PRIORITY.put(tileId, priority);
        }
    }

    public static void registerJigsawTile(ResourceLocation jigsawPattern, int priority, ResourceLocation tileID, Setter setter) {
        registerJigsawTile(jigsawPattern, priority, Collections.singletonList(tileID), setter);
    }

    public static void registerJigsawTile(ResourceLocation jigsawPattern, int priority, ResourceLocation tileID) {
        registerJigsawTile(jigsawPattern, priority, tileID, ALWAYS);
    }

    public static Setter setterByName(String setter, ResourceLocation resourceId) {
        return switch (setter) {
            case "always" -> StructureHandler.ALWAYS;
            case "if_x_direction" -> StructureHandler::IF_X_DIRECTION;
            case "if_z_direction" -> StructureHandler::IF_Z_DIRECTION;
            case "span_x" -> StructureHandler::SPAN_X;
            case "span_z" -> StructureHandler::SPAN_Z;
            case "bridge_end_x" -> StructureHandler::BRIDGE_END_X;
            case "bridge_end_z" -> StructureHandler::BRIDGE_END_Z;
            case "above_ground" -> StructureHandler::ABOVE_GROUND;
            default -> throw new IllegalArgumentException("Unknown setter `" + setter + "` in " + resourceId);
        };
    }

    private static int getPriority(ResourceLocation structurePieceId) {
        return AntiqueAtlas.tileSelectionRules.getStructurePriority(
                structurePieceId,
                null,
                STRUCTURE_PIECE_TILE_PRIORITY.getOrDefault(structurePieceId, Integer.MAX_VALUE)
        );
    }

    private static void put(Level world, int chunkX, int chunkZ, ResourceLocation textureId) {
        ResourceLocation existingTile = AtlasAPI.getTileAPI().getGlobalTile(world, chunkX, chunkZ);

        int newPriority = AntiqueAtlas.tileSelectionRules.getStructurePriority(
                textureId,
                world.dimension().location(),
                STRUCTURE_PIECE_TILE_PRIORITY.getOrDefault(textureId, Integer.MAX_VALUE)
        );
        int existingPriority = AntiqueAtlas.tileSelectionRules.getStructurePriority(
                existingTile,
                world.dimension().location(),
                STRUCTURE_PIECE_TILE_PRIORITY.getOrDefault(existingTile, Integer.MAX_VALUE)
        );

        if (newPriority > existingPriority) {
            AtlasAPI.getTileAPI().putGlobalTile(world, textureId, chunkX, chunkZ);
        }
    }

    private static ResourceLocation chooseTileVariant(ServerLevel world, BoundingBox box, ResourceLocation mappingId, List<ResourceLocation> tileIds) {
        if (tileIds.isEmpty()) {
            return null;
        }
        if (tileIds.size() == 1) {
            return tileIds.get(0);
        }

        long hash = world.getSeed();
        hash = hash * 31L + box.getCenter().getX();
        hash = hash * 31L + box.getCenter().getY();
        hash = hash * 31L + box.getCenter().getZ();
        hash = hash * 31L + box.getXSpan();
        hash = hash * 31L + box.getYSpan();
        hash = hash * 31L + box.getZSpan();
        if (mappingId != null) {
            hash = hash * 31L + mappingId.hashCode();
        }

        int index = Math.floorMod((int) (hash ^ (hash >>> 32)), tileIds.size());
        return tileIds.get(index);
    }

    private static void resolveJigsaw(StructurePiece jigsawPiece, ServerLevel world) {
        if (jigsawPiece instanceof PoolElementStructurePiece pool) {
            if (pool.getElement() instanceof SinglePoolElement singlePoolElement) {
                Optional<ResourceLocation> left = singlePoolElement.template.left();

                if (left.isPresent()) {
                    ResourceLocation templateId = left.get();
                    Collection<RegisteredTile> entries = JIGSAW_TO_TILE_MAP.get(templateId);
                    if (entries.isEmpty()) {
                        logMissingJigsawPattern(templateId, jigsawPiece, world);
                    }

                    for (RegisteredTile entry : entries) {
                        ResourceLocation tile = chooseTileVariant(world, pool.getBoundingBox(), templateId, entry.tiles());
                        if (tile == null) {
                            continue;
                        }
                        for (ChunkPos pos : entry.setter().matches(world, singlePoolElement, pool.getBoundingBox(), jigsawPiece)) {
                            put(world, pos.x, pos.z, tile);
                        }
                    }
                }
            }

        }
    }

    public static void resolve(StructurePiece structurePiece, ServerLevel world) {
        if (structurePiece.getType() == StructurePieceType.JIGSAW) {
            resolveJigsaw(structurePiece, world);

            return;
        }

        ResourceLocation structurePieceId = world.registryAccess().registryOrThrow(Registries.STRUCTURE_PIECE).getKey(structurePiece.getType());
        if (STRUCTURE_PIECE_TO_TILE_MAP.containsKey(structurePieceId)) {
            for (RegisteredTile entry : STRUCTURE_PIECE_TO_TILE_MAP.get(structurePieceId)) {
                Collection<ChunkPos> matches;
                if (structurePiece instanceof PoolElementStructurePiece pool) {
                    matches = entry.setter().matches(world, pool.getElement(), pool.getBoundingBox(), structurePiece);
                } else {
                    matches = entry.setter().matches(world, null, structurePiece.getBoundingBox(), structurePiece);
                }

                ResourceLocation tile = chooseTileVariant(world, structurePiece.getBoundingBox(), structurePieceId, entry.tiles());
                if (tile == null) {
                    continue;
                }
                for (ChunkPos pos : matches) {
                    put(world, pos.x, pos.z, tile);
                }
            }
        } else {
            logMissingStructurePiece(structurePieceId, structurePiece, world);
        }
    }

    private static void logMissingStructurePiece(ResourceLocation structurePieceId, StructurePiece structurePiece, ServerLevel world) {
        if (structurePieceId != null && LOGGED_MISSING_STRUCTURE_PIECES.add(structurePieceId)) {
            AntiqueAtlas.LOG.info(
                    "Atlas has no tile mapping for structure piece type {} in dimension {} (piece class: {}). Add it under data/*/atlas/structure_pieces/*.json if needed. Suggested template:{}",
                    structurePieceId,
                    world.dimension().location(),
                    structurePiece.getClass().getName(),
                    buildStructurePieceTemplate(structurePieceId)
            );
        }
    }

    private static void logMissingJigsawPattern(ResourceLocation templateId, StructurePiece structurePiece, ServerLevel world) {
        if (LOGGED_MISSING_JIGSAW_PATTERNS.add(templateId)) {
            AntiqueAtlas.LOG.info(
                    "Atlas has no jigsaw tile mapping for template {} in dimension {} (piece class: {}). Add it under data/*/atlas/structures/... if needed. Suggested template:{}",
                    templateId,
                    world.dimension().location(),
                    structurePiece.getClass().getName(),
                    buildJigsawTemplate(templateId)
            );
        }
    }

    private static String buildStructurePieceTemplate(ResourceLocation structurePieceId) {
        return "\n{\n" +
                "  \"version\": 1,\n" +
                "  \"piece_type\": \"" + structurePieceId + "\",\n" +
                "  \"tile\": \"navigate:replace_me\",\n" +
                "  \"priority\": 100,\n" +
                "  \"setter\": \"always\"\n" +
                "}";
    }

    private static String buildJigsawTemplate(ResourceLocation templateId) {
        return "\n{\n" +
                "  \"version\": 1,\n" +
                "  \"tile\": \"navigate:replace_me\",\n" +
                "  \"priority\": 100\n" +
                "}\n" +
                "// path example: data/" + templateId.getNamespace() + "/atlas/structures/" + templateId.getPath() + ".json";
    }

    public static void resolve(StructureStart structureStart, ServerLevel world) {
        // Structures are represented as tiles now; automatic non-death markers are intentionally disabled.
    }

    interface Setter {
        Collection<ChunkPos> matches(Level world, StructurePoolElement element, BoundingBox box, StructurePiece rotation);
    }

    private record RegisteredTile(List<ResourceLocation> tiles, Setter setter) {
        private RegisteredTile {
            tiles = List.copyOf(new ArrayList<>(tiles));
        }
    }
}
