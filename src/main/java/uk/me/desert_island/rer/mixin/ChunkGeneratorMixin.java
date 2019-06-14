package uk.me.desert_island.rer.mixin;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {
    private static Map<Integer, Map<Block, Integer>> block_counts_at_level = new HashMap<Integer, Map<Block, Integer>>();
    private static Map<Integer, Integer> total_counts_at_level = new HashMap<Integer, Integer>();
    
    @Inject(at = @At("RETURN"), method = "generateFeatures(Lnet/minecraft/world/ChunkRegion;)V")
    private void generateFeatures(ChunkRegion region, CallbackInfo info) {
	System.out.println("This line is printed by an example mod mixin!");
	
	int cent_chunk_x = region.getCenterChunkX();
	int cent_chunk_z = region.getCenterChunkZ();
	int cent_block_x = cent_chunk_x * 16;
	int cent_block_z = cent_chunk_z * 16;

	System.out.printf("generateFeatures for block %d,%d\n", cent_block_x, cent_block_z);
	
	for (int y=0; y < 128; y++) {
	    /* This could probably be raised up to a constructor? */
	    if (!total_counts_at_level.containsKey(y)) {
		total_counts_at_level.put(y, 0);
	    }
	    
	    if (!block_counts_at_level.containsKey(y)) {
		block_counts_at_level.put(y, new HashMap<Block, Integer>());
	    }
	    Map<Block, Integer> block_counts_at_this_level = block_counts_at_level.get(y);
	    
	    for (int x=cent_block_x - 8; x < cent_block_x + 8; x++) {
		/* use heightmap or something instead of hardcoding this? */
		for (int z=cent_block_z - 8; z < cent_block_z + 8; z++) {
		    
		    total_counts_at_level.put(y, total_counts_at_level.get(y) + 1);
		    
		    Block block = region.getBlockState(new BlockPos(x, y, z)).getBlock();
		    
		    if (!block_counts_at_this_level.containsKey(block)) {
			block_counts_at_this_level.put(block, 1);
		    } else {
			block_counts_at_this_level.put(block, block_counts_at_this_level.get(block) + 1);
		    }
		    
		}
	    }
	}
	
	for (int y=0; y < 128; y++) {
	    int total_count = total_counts_at_level.get(y);
	    //System.out.printf("y=%d\n", y);
	    
	    
	    Map<Block, Integer> block_counts_at_this_level = block_counts_at_level.get(y);
	    for (Block block : block_counts_at_this_level.keySet()) {
		int count = block_counts_at_this_level.get(block);
		//System.out.printf("y=%3d %8.2g%%: %s\n", y, (double)count/total_count * 100.0, block);
	    }
	}
	
	System.out.println();
    }
}
