package uk.me.desert_island.rer.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.WorldGenState;

import java.util.concurrent.atomic.AtomicLongArray;

import static uk.me.desert_island.rer.RoughlyEnoughResources.*;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
    @Inject(at = @At("RETURN"), method = "applyBiomeDecoration")
    private void generateFeatures(WorldGenLevel swa, ChunkAccess chunk, StructureManager structureAccessor, CallbackInfo ci) {
        long startTime = System.nanoTime();

        ServerLevel world = swa.getLevel();
        int centerBlockX = chunk.getPos().getMiddleBlockX();
        int centerBlockZ = chunk.getPos().getMiddleBlockZ();

        RERUtils.LOGGER.debug("generateFeatures for block %d,%d", centerBlockX, centerBlockZ);

        WorldGenState state = WorldGenState.byWorld(world.dimension());

        for (int y = MIN_WORLD_Y; y < MAX_WORLD_Y; y++) {
            for (int x = centerBlockX - 8; x < centerBlockX + 8; x++) {
                /* use heightmap or something instead of hard-coding this? */
                for (int z = centerBlockZ - 8; z < centerBlockZ + 8; z++) {
                    Block block = chunk.getBlockState(new BlockPos(x, y, z)).getBlock();

                    state.totalCountsAtLevelsMap.getAndIncrement(y - MIN_WORLD_Y);

                    AtomicLongArray levelCount = state.levelCountsMap.get(block);
                    if (levelCount == null) {
                        levelCount = new AtomicLongArray(WORLD_HEIGHT);
                        state.levelCountsMap.put(block, levelCount);
                    }

                    levelCount.getAndIncrement(y - MIN_WORLD_Y);
                    state.markPlayerDirty(block);
                }
            }
        }

        state.setDirty();
        long endTime = System.nanoTime();
        RERUtils.LOGGER.debug("RER profiling that chunk took %f ms", (endTime - startTime) / 1e9);
    }
}
