package uk.me.desert_island.rer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.ReentrantLock;

import static uk.me.desert_island.rer.RoughlyEnoughResources.WORLD_HEIGHT;

public class WorldGenState extends PersistentState {
    public static Map<RegistryKey<World>, PersistentStateManager> persistentStateManagerMap = new HashMap<>();
    private static final Logger LOGGER = LogManager.getFormatterLogger("rer-wgs");
    public IntSet playerDirty = new IntOpenHashSet();
    private final ReentrantLock lock = new ReentrantLock();

    public void lockPlayerDirty() {
        lock.lock();
    }

    public void unlockPlayerDirty() {
        lock.unlock();
    }

    public static void registerPsm(PersistentStateManager psm, RegistryKey<World> world) {
        if (persistentStateManagerMap.containsKey(world)) {
            LOGGER.printf(Level.WARN, "Registering psm %s for already known world %s?", psm, world);
        }
        persistentStateManagerMap.put(world, psm);
    }

    public static WorldGenState byWorld(World world) {
        return byWorld(world.getRegistryKey());
    }

    public static WorldGenState byWorld(RegistryKey<World> world) {
        String name = "rer_worldgen";
        PersistentStateManager psm = persistentStateManagerMap.get(world);
        return psm.getOrCreate(nbt -> new WorldGenState(nbt, name, world), () -> new WorldGenState(null, name, world), name);
    }

    public void markPlayerDirty(Block block) {
        lockPlayerDirty();
        this.playerDirty.add(-1);
        this.playerDirty.add(Registry.BLOCK.getRawId(block));
        unlockPlayerDirty();
    }

    public WorldGenState(NbtCompound nbt, String string_1, RegistryKey<World> type) {
        if (nbt != null) {
            fromNbt(nbt);
        }
    }

    public void sendToPlayers(Iterable<ServerPlayerEntity> player, PacketByteBuf infoBuf, RegistryKey<World> world) {
        System.out.println("Size of buffer: " + infoBuf.readableBytes());
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        RoughlyEnoughResources.writeIdentifier(buf, world.getValue());
        buf.writeBytes(infoBuf);

        ByteBuf start_bb = Unpooled.buffer().writeInt(buf.readableBytes());
        PacketByteBuf start_packet = new PacketByteBuf(start_bb);

        PacketByteBuf done_packet = new PacketByteBuf(Unpooled.buffer());


        for (ServerPlayerEntity entity : player) {
            ServerPlayNetworking.send(entity, RoughlyEnoughResources.SEND_WORLD_GEN_STATE_START, start_packet);

            PacketByteBuf player_packet = new PacketByteBuf(buf.retainedDuplicate());

            while (player_packet.readableBytes() > 0) {
                int this_size = player_packet.readableBytes();
                if (this_size > 1000000) {
                    this_size = 1000000;
                }
                ByteBuf sub_bb = Unpooled.buffer(this_size);
                player_packet.readBytes(sub_bb, this_size);

                ServerPlayNetworking.send(entity, RoughlyEnoughResources.SEND_WORLD_GEN_STATE_CHUNK, new PacketByteBuf(sub_bb));
            }
            ServerPlayNetworking.send(entity, RoughlyEnoughResources.SEND_WORLD_GEN_STATE_DONE, done_packet);
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
    public void fromNbt(NbtCompound root) {
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
        totalCountsAtLevelsMap = new AtomicLongArray(WORLD_HEIGHT);

        long[] totalCountsAtLevels = root.getLongArray("total_counts_at_level");
        for (int i = 0; i < totalCountsAtLevels.length; i++) {
            totalCountsAtLevelsMap.set(i, totalCountsAtLevels[i]);
        }

        NbtCompound levelCountsForBlock = root.getCompound("level_counts_for_block");
        for (String blockIdString : levelCountsForBlock.getKeys()) {
            Block block = Registry.BLOCK.get(new Identifier(blockIdString));
            levelCountsMap.put(block, new AtomicLongArray(WORLD_HEIGHT));
            AtomicLongArray levelCount = levelCountsMap.get(block);
            long[] countsForBlockTag = levelCountsForBlock.getLongArray(blockIdString);
            for (int i = 0; i < countsForBlockTag.length; i++) {
                levelCount.set(i, countsForBlockTag[i]);
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound rootTag) {
        rootTag.putInt("Version", 0);

        long[] totalCountsAtLevelArray = new long[totalCountsAtLevelsMap.length()];
        for (int i = 0; i < totalCountsAtLevelArray.length; i++) {
            totalCountsAtLevelArray[i] = totalCountsAtLevelsMap.get(i);
        }
        rootTag.putLongArray("total_counts_at_level", totalCountsAtLevelArray);

        NbtCompound tag = new NbtCompound();
        for (Block block : levelCountsMap.keySet()) {
            AtomicLongArray countsForBlockMap = levelCountsMap.get(block);
            long[] countsForBlockTag = new long[countsForBlockMap.length()];
            for (int i = 0; i < countsForBlockTag.length; i++) {
                countsForBlockTag[i] = countsForBlockMap.get(i);
            }
            tag.putLongArray(Registry.BLOCK.getId(block).toString(), countsForBlockTag);
        }
        rootTag.put("level_counts_for_block", tag);

        return rootTag;
    }

    private static void writeVarLongArray(PacketByteBuf buf, long[] array) {
        buf.writeVarInt(array.length);
        int length = array.length;

        for (int var4 = 0; var4 < length; ++var4) {
            buf.writeVarLong(array[var4]);
        }
    }

    public PacketByteBuf toNetwork(boolean append, PacketByteBuf buf, IntSet allLevels) {
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
            Block block = Registry.BLOCK.get(level);
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
            levels.add(Registry.BLOCK.getRawId(block));
        }
        return levels;
    }
}
