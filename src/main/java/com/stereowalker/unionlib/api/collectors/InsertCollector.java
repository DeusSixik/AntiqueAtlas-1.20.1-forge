package com.stereowalker.unionlib.api.collectors;

import com.stereowalker.unionlib.insert.ClientInserts;
import com.stereowalker.unionlib.insert.Inserts;
import hunternif.mc.impl.atlas.structure.StructureInsertDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InsertCollector {
    private final List<Consumer<Level>> levelLoadHandlers = new ArrayList<>();
    private final List<Consumer<Player>> loginHandlers = new ArrayList<>();
    private final List<Consumer<LivingEntity>> livingTickHandlers = new ArrayList<>();
    private final List<Runnable> clientTickFinishHandlers = new ArrayList<>();

    public void addInsert(Inserts insert, Consumer<?> consumer) {
        switch (insert) {
            case LEVEL_LOAD -> levelLoadHandlers.add((Consumer<Level>) consumer);
            case LOGGED_IN -> loginHandlers.add((Consumer<Player>) consumer);
            case LIVING_TICK -> livingTickHandlers.add((Consumer<LivingEntity>) consumer);
            default -> throw new IllegalArgumentException("Unsupported consumer insert: " + insert);
        }
    }

    public void addInsert(Inserts insert, StructurePieceHandler handler) {
        if (insert != Inserts.STRUCTURE_PIECE_ADDED) {
            throw new IllegalArgumentException("Wrong insert for structure piece handler: " + insert);
        }
        StructureInsertDispatcher.addStructurePieceHandler(handler);
    }

    public void addInsert(Inserts insert, StructureStartHandler handler) {
        if (insert != Inserts.STRUCTURE_ADDED) {
            throw new IllegalArgumentException("Wrong insert for structure start handler: " + insert);
        }
        StructureInsertDispatcher.addStructureStartHandler(handler);
    }

    public void addInsert(ClientInserts insert, Runnable runnable) {
        if (insert != ClientInserts.CLIENT_TICK_FINISH) {
            throw new IllegalArgumentException("Unsupported client insert: " + insert);
        }
        clientTickFinishHandlers.add(runnable);
    }

    public List<Consumer<Level>> levelLoadHandlers() {
        return levelLoadHandlers;
    }

    public List<Consumer<Player>> loginHandlers() {
        return loginHandlers;
    }

    public List<Consumer<LivingEntity>> livingTickHandlers() {
        return livingTickHandlers;
    }

    public List<Runnable> clientTickFinishHandlers() {
        return clientTickFinishHandlers;
    }

    @FunctionalInterface
    public interface StructurePieceHandler {
        void accept(StructurePiece structurePiece, ServerLevel world);
    }

    @FunctionalInterface
    public interface StructureStartHandler {
        void accept(StructureStart structureStart, ServerLevel world);
    }
}
