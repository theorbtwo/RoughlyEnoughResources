package uk.me.desert_island.rer;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.PersistentState;

public class WorldGenState extends PersistentState {
    public static WorldGenState GLOBAL_WORLD_GEN_STATE;

    public WorldGenState(String string_1) {
        super(string_1);

        if (GLOBAL_WORLD_GEN_STATE == null) {
            GLOBAL_WORLD_GEN_STATE = this;
        }
    }

    public static Map<Block, Map<Integer, Long>> level_counts_for_block = new HashMap<Block, Map<Integer, Long>>();
    public static Map<Integer, Long> total_counts_at_level = new HashMap<Integer, Long>(128);
    public final int CURRENT_VERSION = 0;

    /* 
     * NBT format:
     * Version: 0
     * 
     */

    @Override
    public void fromTag(CompoundTag root) {
        int version;
        if (!root.containsKey("Version", NbtType.INT)) {
            System.out.println("Invalid save data. Expected a Version, found no Version, throwing out existing data in a huff.");
            return;
        } else {
            version = root.getInt("Version");
        }

        if (version < 0 || version > 0) {
            System.out.println("Invalid save data. Expected Version 0, found " + version + ". Discarding save data.");
            return;
        }

        level_counts_for_block.clear();
        total_counts_at_level.clear();

        /*
        long[] tcal_array = root.getLongArray("total_counts_at_level");
        for (Integer i = 0; i < tcal_array.length; i++) {
            total_counts_at_level.put(i, tcal_array[i]);
        }

        CompoundTag bcal_ctag = root.getCompound("level_counts_for_block");
        for (String block_id : bcal_ctag.getKeys()) {
            level_counts_for_block.put(Registry.BLOCK.get());
        }
        */
    }

    @Override
    public CompoundTag toTag(CompoundTag root) {
        root.putInt("Version", 0);
        

        /*
        for (String entity_id : rankings.keySet()) {
            CompoundTag entity_tag = new CompoundTag();
            entity_tag.putDouble("rank", rankings.get(entity_id));
            entities.put(entity_id, entity_tag);
        }
        */

        return root;
    }

    // Returns 0 if the real result is undefined.
    public static double get_portion_at_height(Block block, int y) {
        Map<Integer, Long> this_lcfb = level_counts_for_block.getOrDefault(block, null);
        
        if (this_lcfb == null) {
            return 0;
        }

        if (!this_lcfb.containsKey(y)) {
            return 0;
        }

        long block_count = this_lcfb.get(y);

        long total_count = total_counts_at_level.getOrDefault(y, (long) 1);

        if (total_count == 0) {
            return 0;
        }

        return (double)block_count/(double)total_count;
    }

	public static double get_max_portion(Block block) {
        double max_portion=0.0;

        for (int y=0; y<128; y++) {
            max_portion = Math.max(max_portion, get_portion_at_height(block, y));
        }
		return max_portion;
	}
}
