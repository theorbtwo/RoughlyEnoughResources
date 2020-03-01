package uk.me.desert_island.rer.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ClientWorldGenState {
    public static Map<DimensionType, ClientWorldGenState> dimensionTypeStateMap = new HashMap<>();

    public Map<Block, Map<Integer, Long>> levelCountsMap = new ConcurrentHashMap<>();
    public Map<Integer, Long> totalCountsAtLevelsMap = new ConcurrentHashMap<>(128);

    public static ClientWorldGenState byDimension(Dimension dim) {
        return byDimension(dim.getType());
    }

    public static ClientWorldGenState byDimension(DimensionType dim) {
        ClientWorldGenState state = dimensionTypeStateMap.get(dim);
        if (state == null) {
            dimensionTypeStateMap.put(dim, new ClientWorldGenState());
            return dimensionTypeStateMap.get(dim);
        }
        return state;
    }

    public void readFromServerTag(CompoundTag root) {
        int version;
        if (!root.contains("Version", NbtType.INT)) {
            System.out.println("Invalid save data. Expected a Version, found no Version, throwing out existing data in a huff.");
            return;
        } else {
            version = root.getInt("Version");
        }

        if (version != 0) {
            System.out.println("Invalid save data. Expected Version 0, found " + version + ". Discarding save data.");
            return;
        }

        levelCountsMap.clear();
        totalCountsAtLevelsMap.clear();

        long[] totalCountsAtLevels = root.getLongArray("total_counts_at_level");
        for (int i = 0; i < totalCountsAtLevels.length; i++) {
            totalCountsAtLevelsMap.put(i, totalCountsAtLevels[i]);
        }

        CompoundTag levelCountsForBlock = root.getCompound("level_counts_for_block");
        for (String blockIdString : levelCountsForBlock.getKeys()) {
            Block block = Registry.BLOCK.get(new Identifier(blockIdString));
            levelCountsMap.put(block, new ConcurrentHashMap<>(128));
            Map<Integer, Long> levelCount = levelCountsMap.get(block);
            long[] countsForBlockTag = levelCountsForBlock.getLongArray(blockIdString);
            for (int i = 0; i < 128; i++) {
                levelCount.put(i, countsForBlockTag[i]);
            }
        }
    }

    // Returns 0 if the real result is undefined.
    public double getPortionAtHeight(Block block, int y) {
        Map<Integer, Long> levelCount = levelCountsMap.getOrDefault(block, null);

        if (levelCount == null) {
            return 0;
        }

        if (!levelCount.containsKey(y)) {
            return 0;
        }

        double block_count = levelCount.get(y);

        double total_count = totalCountsAtLevelsMap.getOrDefault(y, (long) 1);

        if (total_count == 0) {
            return 0;
        }

        return block_count / total_count;
    }

    public double getMaxPortion(Block block) {
        double maxPortion = 0.0;

        for (int y = 0; y < 128; y++) {
            maxPortion = Math.max(maxPortion, getPortionAtHeight(block, y));
        }
        return maxPortion;
    }
}
