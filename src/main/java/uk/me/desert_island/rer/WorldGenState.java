package uk.me.desert_island.rer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class WorldGenState extends PersistentState {
    public static Map<RegistryKey<World>, PersistentStateManager> persistentStateManagerMap = new HashMap<>();
    private static final Logger LOGGER = LogManager.getFormatterLogger("rer-wgs");
    public IntSet playerDirty = new IntOpenHashSet();
    private ReentrantLock lock = new ReentrantLock();

    public void lockPlayerDirty()
    {
        lock.lock();
    }
    public void unlockPlayerDirty()
    {
        lock.unlock();
    }

    public static void registerPsm(PersistentStateManager psm, RegistryKey<World> world) {
        if (persistentStateManagerMap.containsKey(world)) {
            LOGGER.warn("Registering psm %s for already known world %s?", psm, world);
        }
        persistentStateManagerMap.put(world, psm);
    }

    public static WorldGenState byWorld(World world) {
        return byWorld(world.getRegistryKey());
    }

    public static WorldGenState byWorld(RegistryKey<World> world) {
        String name = "rer_worldgen";
        PersistentStateManager psm = persistentStateManagerMap.get(world);
        return psm.getOrCreate(() -> new WorldGenState(name, world), name);
    }

    public void markPlayerDirty(Block block) {
        lockPlayerDirty();
        this.playerDirty.add(-1);
        this.playerDirty.add(Registry.BLOCK.getRawId(block));
        unlockPlayerDirty();
    }

    public WorldGenState(String string_1, RegistryKey<World> type) {
        super(string_1);
    }

    public void sendToPlayers(Iterable<ServerPlayerEntity> player, PacketByteBuf infoBuf, RegistryKey<World> world) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer()).writeIdentifier(world.getValue());
        buf.writeBytes(infoBuf);
        for (ServerPlayerEntity entity : player) {
            ServerSidePacketRegistry.INSTANCE.sendToPlayer(entity, RoughlyEnoughResources.SEND_WORLD_GEN_STATE, buf);
        }
    }

    public Map<Block, AtomicLongArray> levelCountsMap = new ConcurrentHashMap<>();
    public AtomicLongArray totalCountsAtLevelsMap = new AtomicLongArray(128);
    public final int CURRENT_VERSION = 0;

    /*
     * NBT format:
     * Version: 0
     *
     */

    @Override
    public void fromTag(CompoundTag root) {
        int version;
        if (!root.contains("Version", NbtType.INT)) {
            RERUtils.LOGGER.error("Invalid save data. Expected a Version, found no Version, throwing out existing data in a huff.");
            return;
        } else {
            version = root.getInt("Version");
        }

        if (version != 0) {
            RERUtils.LOGGER.error("Invalid save data. Expected Version 0, found " + version + ". Discarding save data.");
            return;
        }

        levelCountsMap.clear();
        totalCountsAtLevelsMap = new AtomicLongArray(128);

        long[] totalCountsAtLevels = root.getLongArray("total_counts_at_level");
        for (int i = 0; i < totalCountsAtLevels.length; i++) {
            totalCountsAtLevelsMap.set(i, totalCountsAtLevels[i]);
        }

        CompoundTag levelCountsForBlock = root.getCompound("level_counts_for_block");
        for (String blockIdString : levelCountsForBlock.getKeys()) {
            Block block = Registry.BLOCK.get(new Identifier(blockIdString));
            levelCountsMap.put(block, new AtomicLongArray(128));
            AtomicLongArray levelCount = levelCountsMap.get(block);
            long[] countsForBlockTag = levelCountsForBlock.getLongArray(blockIdString);
            for (int i = 0; i < 128; i++) {
                levelCount.set(i, countsForBlockTag[i]);
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag rootTag) {
        rootTag.putInt("Version", 0);

        long[] totalCountsAtLevelArray = new long[128];
        for (int i = 0; i < 128; i++) {
            totalCountsAtLevelArray[i] = totalCountsAtLevelsMap.get(i);
        }
        rootTag.putLongArray("total_counts_at_level", totalCountsAtLevelArray);

        CompoundTag tag = new CompoundTag();
        for (Block block : levelCountsMap.keySet()) {
            AtomicLongArray countsForBlockMap = levelCountsMap.get(block);
            long[] countsForBlockTag = new long[128];
            for (int i = 0; i < 128; i++) {
                countsForBlockTag[i] = countsForBlockMap.get(i);
            }
            tag.putLongArray(Registry.BLOCK.getId(block).toString(), countsForBlockTag);
        }
        rootTag.put("level_counts_for_block", tag);

        return rootTag;
    }

    public PacketByteBuf toNetwork(boolean append, PacketByteBuf buf, IntSet allLevels) {
        buf.writeBoolean(append);
        if (allLevels.contains(-1)) {
            long[] totalCountsAtLevelArray = new long[128];
            for (int i = 0; i < 128; i++) {
                totalCountsAtLevelArray[i] = totalCountsAtLevelsMap.get(i);
            }
            buf.writeLongArray(totalCountsAtLevelArray);
            allLevels.remove(-1);
        }
        for (int level : allLevels) {
            Block block = Registry.BLOCK.get(level);
            AtomicLongArray countsForBlockMap = levelCountsMap.get(block);

            if (countsForBlockMap != null) {
                long[] countsForBlockTag = new long[128];
                for (int i = 0; i < 128; i++) {
                    countsForBlockTag[i] = countsForBlockMap.get(i);
                }

                buf.writeInt(level);
                buf.writeLongArray(countsForBlockTag);
            }
        }

        return buf;
    }

    public IntSet buildEverythingLevels() {
        IntSet levels = new IntOpenHashSet();
        levels.add(-1);
        for (Block block : levelCountsMap.keySet()) {
            levels.add(Registry.BLOCK.getRawId(block));
        }
        return levels;
    }
}
