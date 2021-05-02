package uk.me.desert_island.rer.mixin;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
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
    private void generateFeatures(ChunkRegion region, StructureAccessor structureAccessor, CallbackInfo ci) {
        long startTime = System.nanoTime();
        int centerChunkX = region.getCenterChunkX();
        int centerChunkZ = region.getCenterChunkZ();
        int centerBlockX = centerChunkX * 16;
        int centerBlockZ = centerChunkZ * 16;

        RERUtils.LOGGER.debug("generateFeatures for block %d,%d", centerBlockX, centerBlockZ);

        WorldGenState state = WorldGenState.byWorld(region.toServerWorld().getRegistryKey());

        for (int y = 0; y < 128; y++) {
            for (int x = centerBlockX - 8; x < centerBlockX + 8; x++) {
                /* use heightmap or something instead of hardcoding this? */
                for (int z = centerBlockZ - 8; z < centerBlockZ + 8; z++) {
                    Block block = region.getBlockState(new BlockPos(x, y, z)).getBlock();

                    state.totalCountsAtLevelsMap.set(y, state.totalCountsAtLevelsMap.get(y) + 1);

                    AtomicLongArray levelCount = state.levelCountsMap.get(block);
                    if (levelCount == null) {
                        levelCount = new AtomicLongArray(128);
                        state.levelCountsMap.put(block, levelCount);
                    }

                    levelCount.set(y, levelCount.get(y) + 1);
                    
                    state.markPlayerDirty(block);
                }
            }
        }

        state.markDirty();
        long endTime = System.nanoTime();
        RERUtils.LOGGER.debug("RER profiling that chunk took %f ms", (endTime - startTime) / 1e9);
    }
}
