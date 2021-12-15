package uk.me.desert_island.rer.mixin;

import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.WorldGenState;

import java.util.concurrent.atomic.AtomicLongArray;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
    @Inject(at = @At("RETURN"), method = "generateFeatures")
    private void generateFeatures(StructureWorldAccess swa, Chunk chunk, StructureAccessor structureAccessor,
                                  CallbackInfo ci) {
        long startTime = System.nanoTime();

        ServerWorld world = swa.toServerWorld();
        int centerBlockX = chunk.getPos().getCenterX();
        int centerBlockZ = chunk.getPos().getCenterZ();

        RERUtils.LOGGER.debug("generateFeatures for block %d,%d", centerBlockX, centerBlockZ);

        WorldGenState state = WorldGenState.byWorld(world.getRegistryKey());

        for (int y = 0; y < 128; y++) {
            for (int x = centerBlockX - 8; x < centerBlockX + 8; x++) {
                /* use heightmap or something instead of hardcoding this? */
                for (int z = centerBlockZ - 8; z < centerBlockZ + 8; z++) {
                    Block block = chunk.getBlockState(new BlockPos(x, y, z)).getBlock();

                    state.totalCountsAtLevelsMap.getAndIncrement(y);

                    AtomicLongArray levelCount = state.levelCountsMap.get(block);
                    if (levelCount == null) {
                        levelCount = new AtomicLongArray(128);
                        state.levelCountsMap.put(block, levelCount);
                    }

                    levelCount.getAndIncrement(y);
                    
                    state.markPlayerDirty(block);
                }
            }
        }

        state.markDirty();
        long endTime = System.nanoTime();
        RERUtils.LOGGER.debug("RER profiling that chunk took %f ms", (endTime - startTime) / 1e9);
    }
}
