package uk.me.desert_island.rer;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.*;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditions;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.LootEntries;
import net.minecraft.loot.entry.LootEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctions;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.BoundedIntUnaryOperator;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RoughlyEnoughResources implements ModInitializer {
    private static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(UniformLootTableRange.class, new UniformLootTableRange.Serializer()).registerTypeAdapter(BinomialLootTableRange.class, new BinomialLootTableRange.Serializer()).registerTypeAdapter(ConstantLootTableRange.class, new ConstantLootTableRange.Serializer()).registerTypeAdapter(BoundedIntUnaryOperator.class, new BoundedIntUnaryOperator.Serializer()).registerTypeAdapter(LootPool.class, new LootPool.Serializer()).registerTypeAdapter(LootTable.class, new LootTable.Serializer()).registerTypeHierarchyAdapter(LootEntry.class, new LootEntries.Serializer()).registerTypeHierarchyAdapter(LootFunction.class, new LootFunctions.Factory()).registerTypeHierarchyAdapter(LootCondition.class, new LootConditions.Factory()).registerTypeHierarchyAdapter(LootContext.EntityTarget.class, new LootContext.EntityTarget.Serializer()).create();

    public static final Identifier SEND_WORLD_GEN_STATE = new Identifier("roughlyenoughresources", "swds");
    public static final Identifier SEND_LOOT_INFO = new Identifier("roughlyenoughresources", "sli");
    public static final Identifier ASK_SYNC_INFO = new Identifier("roughlyenoughresources", "asi");

    @Override
    public void onInitialize() {
        RERUtils.LOGGER.info("Hello Fabric world!");
        ServerSidePacketRegistry.INSTANCE.register(ASK_SYNC_INFO, (packetContext, packetByteBuf) -> {
            packetContext.getTaskQueue().submit(() -> {
                ServerPlayerEntity player = (ServerPlayerEntity) packetContext.getPlayer();
                sendLootToPlayers(player.getServer(), Collections.singletonList(player));
            });
        });
    }

    public static void sendLootToPlayers(MinecraftServer server, List<ServerPlayerEntity> players) {
        System.out.println("sending loot to " + players.stream().map(PlayerEntity::getName).map(Text::getString).collect(Collectors.joining(", ")));
        LootManager lootManager = server.getLootManager();
        List<Identifier> names = Lists.newArrayList(lootManager.getSupplierNames());

        int size = 100;
        for (int i = 0; i < names.size(); i += size) {
            int end = Math.min(names.size(), i + size);
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(end - i);
            for (int j = i; j < end; j++) {
                Identifier identifier = names.get(j);
                LootTable table = lootManager.getSupplier(identifier);
                buf.writeIdentifier(identifier).writeString(GSON.toJson(table));
            }
            Packet<?> packet = ServerSidePacketRegistry.INSTANCE.toPacket(RoughlyEnoughResources.SEND_LOOT_INFO, buf);
            for (ServerPlayerEntity player : players) {
                player.networkHandler.sendPacket(packet);
            }
        }
    }
}
