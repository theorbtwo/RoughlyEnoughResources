package uk.me.desert_island.rer.mixin;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.WorldGenState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
    @Inject(at = @At("RETURN"), method = "generateFeatures(Lnet/minecraft/world/ChunkRegion;)V")
    private void generateFeatures(ChunkRegion region, CallbackInfo info) {
        long startTime = System.nanoTime();
        int centerChunkX = region.getCenterChunkX();
        int centerChunkZ = region.getCenterChunkZ();
        int centerBlockX = centerChunkX * 16;
        int centerBlockZ = centerChunkZ * 16;

        RERUtils.LOGGER.debug("generateFeatures for block %d,%d", centerBlockX, centerBlockZ);

        WorldGenState state = WorldGenState.byDimension(region.getDimension());

        for (int y = 0; y < 128; y++) {
            for (int x = centerBlockX - 8; x < centerBlockX + 8; x++) {
                /* use heightmap or something instead of hardcoding this? */
                for (int z = centerBlockZ - 8; z < centerBlockZ + 8; z++) {
                    Block block = region.getBlockState(new BlockPos(x, y, z)).getBlock();
                    //LOGGER.info("at (%d, %d, %d), got %s", x, y, z, block);

                    state.totalCountsAtLevelsMap.put(y, state.totalCountsAtLevelsMap.getOrDefault(y, 0L) + 1);

                    Map<Integer, Long> levelCount = state.levelCountsMap.get(block);
                    if (levelCount == null) {
                        levelCount = new ConcurrentHashMap<>(128);
                        state.levelCountsMap.put(block, levelCount);
                    }

                    levelCount.put(y, levelCount.getOrDefault(y, 0L) + 1);
                }
            }
        }

        state.markDirty();
        state.markPlayerDirty();
        long endTime = System.nanoTime();
        RERUtils.LOGGER.debug("RER profiling that chunk took %f ms", (endTime - startTime) / 1e9);
    }
}
