package uk.me.desert_island.rer.mixin;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import static uk.me.desert_island.rer.WorldGenState.level_counts_for_block;
import static uk.me.desert_island.rer.WorldGenState.total_counts_at_level;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {

	@Inject(at = @At("RETURN"), method = "generateFeatures(Lnet/minecraft/world/ChunkRegion;)V")
	private void generateFeatures(ChunkRegion region, CallbackInfo info) {

		int cent_chunk_x = region.getCenterChunkX();
		int cent_chunk_z = region.getCenterChunkZ();
		int cent_block_x = cent_chunk_x * 16;
		int cent_block_z = cent_chunk_z * 16;

		System.out.printf("generateFeatures for block %d,%d\n", cent_block_x, cent_block_z);

		for (int y=0; y < 128; y++) {
			for (int x=cent_block_x - 8; x < cent_block_x + 8; x++) {
				/* use heightmap or something instead of hardcoding this? */
				for (int z=cent_block_z - 8; z < cent_block_z + 8; z++) {
					Block block = region.getBlockState(new BlockPos(x, y, z)).getBlock();
					
					total_counts_at_level.put(y, total_counts_at_level.getOrDefault(y, 0L) + 1);

					Map<Integer, Long> this_lcfb = level_counts_for_block.get(block);
					if (this_lcfb == null) {
						this_lcfb = new HashMap<Integer, Long>(128);
						level_counts_for_block.put(block, this_lcfb);
					}

					this_lcfb.put(y, this_lcfb.getOrDefault(y, 0L)+1);
				}
			}
		}
	}
}
