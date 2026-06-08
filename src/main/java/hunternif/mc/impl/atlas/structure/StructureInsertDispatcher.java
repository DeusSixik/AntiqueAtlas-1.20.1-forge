package hunternif.mc.impl.atlas.structure;

import com.stereowalker.unionlib.api.collectors.InsertCollector.StructurePieceHandler;
import com.stereowalker.unionlib.api.collectors.InsertCollector.StructureStartHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StructureInsertDispatcher {
    private static final List<StructurePieceHandler> PIECE_HANDLERS = new CopyOnWriteArrayList<>();
    private static final List<StructureStartHandler> START_HANDLERS = new CopyOnWriteArrayList<>();

    private StructureInsertDispatcher() {
    }

    public static void addStructurePieceHandler(StructurePieceHandler handler) {
        PIECE_HANDLERS.add(handler);
    }

    public static void addStructureStartHandler(StructureStartHandler handler) {
        START_HANDLERS.add(handler);
    }

    public static void onStructurePieceAdded(StructurePiece piece, ServerLevel level) {
        for (StructurePieceHandler handler : PIECE_HANDLERS) {
            handler.accept(piece, level);
        }
    }

    public static void onStructureStartAdded(StructureStart structureStart, ServerLevel level) {
        for (StructureStartHandler handler : START_HANDLERS) {
            handler.accept(structureStart, level);
        }
    }
}
