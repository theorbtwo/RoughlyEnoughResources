package uk.me.desert_island.rer.mixin;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import uk.me.desert_island.rer.WorldGenState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {
    private static final Logger LOGGER;

	static {
		LOGGER = LogManager.getFormatterLogger("rer-cg");
	}

	@Inject(at = @At("RETURN"), method = "generateFeatures(Lnet/minecraft/world/ChunkRegion;)V")
	private void generateFeatures(ChunkRegion region, CallbackInfo info) {
		long start_time = System.nanoTime();
		int cent_chunk_x = region.getCenterChunkX();
		int cent_chunk_z = region.getCenterChunkZ();
		int cent_block_x = cent_chunk_x * 16;
		int cent_block_z = cent_chunk_z * 16;

		LOGGER.info("generateFeatures for block %d,%d", cent_block_x, cent_block_z);
		
		WorldGenState state = WorldGenState.byDimension(region.getDimension());

		for (int y = 0; y < 128; y++) {
			for (int x = cent_block_x - 8; x < cent_block_x + 8; x++) {
				/* use heightmap or something instead of hardcoding this? */
				for (int z = cent_block_z - 8; z < cent_block_z + 8; z++) {
					Block block = region.getBlockState(new BlockPos(x, y, z)).getBlock();
					//LOGGER.info("at (%d, %d, %d), got %s", x, y, z, block);

					state.total_counts_at_level.put(y, state.total_counts_at_level.getOrDefault(y, 0L) + 1);

					Map<Integer, Long> this_lcfb = state.level_counts_for_block.get(block);
					if (this_lcfb == null) {
						this_lcfb = new ConcurrentHashMap<Integer, Long>(128);
						state.level_counts_for_block.put(block, this_lcfb);
					}

					this_lcfb.put(y, this_lcfb.getOrDefault(y, 0L)+1);
				}
			}
		}

		//LOGGER.info("calling markDirty");
		state.markDirty();
		//LOGGER.info("markDirty done");
		long end_time = System.nanoTime();
		LOGGER.info("RER profiling that chunk took %f ms", (end_time - start_time)/1e9);
	}
}
