package hunternif.mc.impl.atlas.mixin;

import hunternif.mc.impl.atlas.structure.StructureInsertDispatcher;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureStart.class)
public abstract class StructureStartMixin {
    @Inject(method = "placeInChunk", at = @At("HEAD"))
    private void antiqueatlas$resolveTiles(WorldGenLevel level,
                                           StructureManager structureManager,
                                           ChunkGenerator generator,
                                           RandomSource random,
                                           BoundingBox box,
                                           ChunkPos chunkPos,
                                           CallbackInfo ci) {
        StructureStart structureStart = (StructureStart) (Object) this;
        if (!structureStart.isValid()) {
            return;
        }

        StructureInsertDispatcher.onStructureStartAdded(structureStart, level.getLevel());
        for (StructurePiece piece : structureStart.getPieces()) {
            if (piece.getBoundingBox().intersects(box)) {
                StructureInsertDispatcher.onStructurePieceAdded(piece, level.getLevel());
            }
        }
    }
}
