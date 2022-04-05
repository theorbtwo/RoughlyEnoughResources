package uk.me.desert_island.rer;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.ReentrantLock;

import static uk.me.desert_island.rer.RoughlyEnoughResources.WORLD_HEIGHT;

public class WorldGenState extends SavedData {
    public static Map<ResourceKey<Level>, DimensionDataStorage> persistentStateManagerMap = new HashMap<>();
    private static final Logger LOGGER = LogManager.getFormatterLogger("rer-wgs");
    public IntSet playerDirty = new IntOpenHashSet();
    private final ReentrantLock lock = new ReentrantLock();

    public void lockPlayerDirty() {
        lock.lock();
    }

    public void unlockPlayerDirty() {
        lock.unlock();
    }

    public static void registerPsm(DimensionDataStorage psm, ResourceKey<Level> world) {
        if (persistentStateManagerMap.containsKey(world)) {
            LOGGER.printf(org.apache.logging.log4j.Level.WARN, "Registering psm %s for already known world %s?", psm, world);
        }
        persistentStateManagerMap.put(world, psm);
    }

    public static WorldGenState byWorld(Level world) {
        return byWorld(world.dimension());
    }

    public static WorldGenState byWorld(ResourceKey<Level> world) {
        String name = "rer_worldgen";
        DimensionDataStorage psm = persistentStateManagerMap.get(world);
        return psm.computeIfAbsent(nbt -> new WorldGenState(nbt, name, world), () -> new WorldGenState(null, name, world), name);
    }

    public void markPlayerDirty(Block block) {
        lockPlayerDirty();
        this.playerDirty.add(-1);
        this.playerDirty.add(Registry.BLOCK.getId(block));
        unlockPlayerDirty();
    }

    public WorldGenState(CompoundTag nbt, String string_1, ResourceKey<Level> type) {
        if (nbt != null) {
            fromNbt(nbt);
        }
    }

    public void sendToPlayers(Iterable<ServerPlayer> player, FriendlyByteBuf infoBuf, ResourceKey<Level> world) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RoughlyEnoughResources.writeIdentifier(buf, world.location());
        buf.writeBytes(infoBuf);

        ByteBuf start_bb = Unpooled.buffer().writeInt(buf.readableBytes());
        FriendlyByteBuf start_packet = new FriendlyByteBuf(start_bb);

        FriendlyByteBuf done_packet = new FriendlyByteBuf(Unpooled.buffer());


        for (ServerPlayer entity : player) {
            NetworkManager.sendToPlayer(entity, RoughlyEnoughResources.SEND_WORLD_GEN_STATE_START, start_packet);

            FriendlyByteBuf player_packet = new FriendlyByteBuf(buf.retainedDuplicate());

            while (player_packet.readableBytes() > 0) {
                int this_size = player_packet.readableBytes();
                if (this_size > 1000000) {
                    this_size = 1000000;
                }
                ByteBuf sub_bb = Unpooled.buffer(this_size);
                player_packet.readBytes(sub_bb, this_size);

                NetworkManager.sendToPlayer(entity, RoughlyEnoughResources.SEND_WORLD_GEN_STATE_CHUNK, new FriendlyByteBuf(sub_bb));
            }
            NetworkManager.sendToPlayer(entity, RoughlyEnoughResources.SEND_WORLD_GEN_STATE_DONE, done_packet);
        }
    }

    public Map<Block, AtomicLongArray> levelCountsMap = new ConcurrentHashMap<>();
    public AtomicLongArray totalCountsAtLevelsMap = new AtomicLongArray(WORLD_HEIGHT);
    public final int CURRENT_VERSION = 0;

    /*
     * NBT format:
     * Version: 0
     *
     */
    public void fromNbt(CompoundTag root) {
        int version;
        if (!root.contains("Version", Tag.TAG_INT)) {
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
        totalCountsAtLevelsMap = new AtomicLongArray(WORLD_HEIGHT);

        long[] totalCountsAtLevels = root.getLongArray("total_counts_at_level");
        for (int i = 0; i < totalCountsAtLevels.length; i++) {
            totalCountsAtLevelsMap.set(i, totalCountsAtLevels[i]);
        }

        CompoundTag levelCountsForBlock = root.getCompound("level_counts_for_block");
        for (String blockIdString : levelCountsForBlock.getAllKeys()) {
            Block block = Registry.BLOCK.get(new ResourceLocation(blockIdString));
            levelCountsMap.put(block, new AtomicLongArray(WORLD_HEIGHT));
            AtomicLongArray levelCount = levelCountsMap.get(block);
            long[] countsForBlockTag = levelCountsForBlock.getLongArray(blockIdString);
            for (int i = 0; i < countsForBlockTag.length; i++) {
                levelCount.set(i, countsForBlockTag[i]);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag rootTag) {
        rootTag.putInt("Version", 0);

        long[] totalCountsAtLevelArray = new long[totalCountsAtLevelsMap.length()];
        for (int i = 0; i < totalCountsAtLevelArray.length; i++) {
            totalCountsAtLevelArray[i] = totalCountsAtLevelsMap.get(i);
        }
        rootTag.putLongArray("total_counts_at_level", totalCountsAtLevelArray);

        CompoundTag tag = new CompoundTag();
        for (Block block : levelCountsMap.keySet()) {
            AtomicLongArray countsForBlockMap = levelCountsMap.get(block);
            long[] countsForBlockTag = new long[countsForBlockMap.length()];
            for (int i = 0; i < countsForBlockTag.length; i++) {
                countsForBlockTag[i] = countsForBlockMap.get(i);
            }
            tag.putLongArray(Registry.BLOCK.getKey(block).toString(), countsForBlockTag);
        }
        rootTag.put("level_counts_for_block", tag);

        return rootTag;
    }

    private static void writeVarLongArray(FriendlyByteBuf buf, long[] array) {
        buf.writeVarInt(array.length);
        int length = array.length;

        for (int var4 = 0; var4 < length; ++var4) {
            buf.writeVarLong(array[var4]);
        }
    }

    public FriendlyByteBuf toNetwork(boolean append, FriendlyByteBuf buf, IntSet allLevels) {
        buf.writeBoolean(append);
        if (allLevels.contains(-1)) {
            long[] totalCountsAtLevelArray = new long[totalCountsAtLevelsMap.length()];
            for (int i = 0; i < totalCountsAtLevelArray.length; i++) {
                totalCountsAtLevelArray[i] = totalCountsAtLevelsMap.get(i);
            }
            writeVarLongArray(buf, totalCountsAtLevelArray);
            allLevels.remove(-1);
        }
        for (int level : allLevels) {
            Block block = Registry.BLOCK.byId(level);
            AtomicLongArray countsForBlockMap = levelCountsMap.get(block);

            if (countsForBlockMap != null) {
                long[] countsForBlockTag = new long[countsForBlockMap.length()];
                for (int i = 0; i < countsForBlockTag.length; i++) {
                    countsForBlockTag[i] = countsForBlockMap.get(i);
                }

                buf.writeVarInt(level);
                writeVarLongArray(buf, countsForBlockTag);
            }
        }

        return buf;
    }

    public IntSet buildEverythingLevels() {
        IntSet levels = new IntOpenHashSet();
        levels.add(-1);
        for (Block block : levelCountsMap.keySet()) {
            levels.add(Registry.BLOCK.getId(block));
        }
        return levels;
    }
}
