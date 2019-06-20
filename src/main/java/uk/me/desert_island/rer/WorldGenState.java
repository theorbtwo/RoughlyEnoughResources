package uk.me.desert_island.rer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
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

    public static Map<Block, Map<Integer, Long>> level_counts_for_block = new ConcurrentHashMap<Block, Map<Integer, Long>>();
    public static Map<Integer, Long> total_counts_at_level = new ConcurrentHashMap<Integer, Long>(128);
    public final int CURRENT_VERSION = 0;

    /* 
     * NBT format:
     * Version: 0
     * 
     */

    @Override
    public synchronized void fromTag(CompoundTag root) {
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

        long[] tcal_array = root.getLongArray("total_counts_at_level");
        for (int i = 0; i < tcal_array.length; i++) {
            total_counts_at_level.put(i, tcal_array[i]);
        }

        CompoundTag lcfb_tag = root.getCompound("level_counts_for_block");
        for (String block_id_str : lcfb_tag.getKeys()) {
            Block block = Registry.BLOCK.get(new Identifier(block_id_str));
            level_counts_for_block.put(block, new ConcurrentHashMap<Integer, Long>(128));
            Map<Integer, Long> this_lcfb = level_counts_for_block.get(block);
            long this_lcfb_tag[] = lcfb_tag.getLongArray(block_id_str);
            for (int i=0; i<128; i++) {
                this_lcfb.put(i, this_lcfb_tag[i]);
            }

        }
    }

    @Override
    public synchronized CompoundTag toTag(CompoundTag root) {
        System.out.printf("toTag\n");
        root.putInt("Version", 0);
        
        long tcal_tag[] = new long[128];
        for (int i=0; i<128; i++) {
            tcal_tag[i] = total_counts_at_level.get(i);
        }
        root.putLongArray("total_counts_at_level", tcal_tag);

        CompoundTag lcfb_tag = new CompoundTag();
        root.put("level_counts_for_block", lcfb_tag);
        for (Block block : level_counts_for_block.keySet()) {
            Map<Integer, Long> this_lcfb_map = level_counts_for_block.get(block);
            long this_lcfb_array[] = new long[128];
            for (int i=0; i<128; i++) {
                this_lcfb_array[i] = this_lcfb_map.getOrDefault(i, (long) 0);
            }
            lcfb_tag.putLongArray(Registry.BLOCK.getId(block).toString(), this_lcfb_array);
        }

        System.out.printf("toTag done\n");
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

        double block_count = this_lcfb.get(y);

        double total_count = total_counts_at_level.getOrDefault(y, (long) 1);

        if (total_count == 0) {
            return 0;
        }

        return block_count/total_count;
    }

	public static double get_max_portion(Block block) {
        double max_portion=0.0;

        for (int y=0; y<128; y++) {
            max_portion = Math.max(max_portion, get_portion_at_height(block, y));
        }
		return max_portion;
	}
}
