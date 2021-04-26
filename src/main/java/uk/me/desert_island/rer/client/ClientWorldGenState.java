package uk.me.desert_island.rer.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;

@Environment(EnvType.CLIENT)
public class ClientWorldGenState {
    public static final Map<RegistryKey<World>, ClientWorldGenState> dimensionTypeStateMap = new ConcurrentHashMap<>();

    public Map<Block, AtomicLongArray> levelCountsMap = new ConcurrentHashMap<>();
    public AtomicLongArray totalCountsAtLevelsMap = new AtomicLongArray(128);

    public static ClientWorldGenState byWorld(World world) {
        return byWorld(world.getRegistryKey());
    }

    public static ClientWorldGenState byWorld(RegistryKey<World> dim) {
        return dimensionTypeStateMap.computeIfAbsent(dim, k -> new ClientWorldGenState());
    }

    public void readFromServerTag(PacketByteBuf buf) {
        boolean isAppend = buf.readBoolean();

        if (!isAppend) {
            totalCountsAtLevelsMap = new AtomicLongArray(buf.readLongArray(null));
        } else {
            long[] totalCountsAtLevels = buf.readLongArray(null);
            for (int i = 0; i < totalCountsAtLevels.length; i++) {
                long atLevel = totalCountsAtLevels[i];
                if (atLevel >= 0)
                    totalCountsAtLevelsMap.set(i, atLevel);
            }
        }

        if (!isAppend) {
            levelCountsMap.clear();
            while (buf.isReadable()) {
                int blockId = buf.readInt();
                Block block = Registry.BLOCK.get(blockId);
                AtomicLongArray levelCount = levelCountsMap.put(block, new AtomicLongArray(buf.readLongArray(null)));
            }
        } else {
            while (buf.isReadable()) {
                int blockId = buf.readInt();
                Block block = Registry.BLOCK.get(blockId);
                AtomicLongArray levelCount = levelCountsMap.get(block);
                if (levelCount == null) {
                    levelCountsMap.put(block, new AtomicLongArray(buf.readLongArray(null)));
                } else {
                    long[] countsForBlockTag = buf.readLongArray(null);
                    for (int i = 0; i < 128; i++) {
                        long l = countsForBlockTag[i];
                        if (l >= 0)
                            levelCount.set(i, l);
                    }
                }
            }
        }
    }

    // Returns 0 if the real result is undefined.
    public double getPortionAtHeight(Block block, int y) {
        AtomicLongArray levelCount = levelCountsMap.getOrDefault(block, null);

        if (levelCount == null) {
            return 0;
        }

        double blockCount = levelCount.get(y);

        double totalCount = totalCountsAtLevelsMap.get(y);

        if (totalCount == 0) {
            return 0;
        }

        return blockCount / totalCount;
    }

    public double getMaxPortion(Block block) {
        double maxPortion = 0.0;

        for (int y = 0; y < 128; y++) {
            maxPortion = Math.max(maxPortion, getPortionAtHeight(block, y));
        }
        return maxPortion;
    }
}
