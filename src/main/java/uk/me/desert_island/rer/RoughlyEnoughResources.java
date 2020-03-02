package uk.me.desert_island.rer;

import com.google.common.collect.Lists;
import com.google.gson.*;
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
import uk.me.desert_island.rer.mixin.IdentifierHooks;

import java.util.*;
import java.util.stream.Collectors;

public class RoughlyEnoughResources implements ModInitializer {
    public static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(UniformLootTableRange.class, new UniformLootTableRange.Serializer()).registerTypeAdapter(BinomialLootTableRange.class, new BinomialLootTableRange.Serializer()).registerTypeAdapter(ConstantLootTableRange.class, new ConstantLootTableRange.Serializer()).registerTypeAdapter(BoundedIntUnaryOperator.class, new BoundedIntUnaryOperator.Serializer()).registerTypeAdapter(LootPool.class, new LootPool.Serializer()).registerTypeAdapter(LootTable.class, new LootTable.Serializer()).registerTypeHierarchyAdapter(LootEntry.class, new LootEntries.Serializer()).registerTypeHierarchyAdapter(LootFunction.class, new LootFunctions.Factory()).registerTypeHierarchyAdapter(LootCondition.class, new LootConditions.Factory()).registerTypeHierarchyAdapter(LootContext.EntityTarget.class, new LootContext.EntityTarget.Serializer()).create();

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

        int size = 50;
        for (int i = 0; i < names.size(); i += size) {
            int end = Math.min(names.size(), i + size);
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(end - i);
            for (int j = i; j < end; j++) {
                Identifier identifier = names.get(j);
                LootTable table = lootManager.getSupplier(identifier);
                buf.writeIdentifier(identifier).writeString(GSON.toJson(optimiseTable(GSON.toJsonTree(table))), 262144);
            }
            for (ServerPlayerEntity player : players) {
                Packet<?> packet = ServerSidePacketRegistry.INSTANCE.toPacket(RoughlyEnoughResources.SEND_LOOT_INFO, new PacketByteBuf(buf.duplicate()));
                player.networkHandler.sendPacket(packet);
            }
        }
    }

    private static JsonElement optimiseTable(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isString()) {
                String s = element.getAsJsonPrimitive().getAsString();
                if (s.length() >= 11 && s.startsWith("minecraft:")) {
                    String substring = s.substring(10);
                    if (IdentifierHooks.isNamespaceValid(substring))
                        return new JsonPrimitive(substring);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                array.set(i, optimiseTable(array.get(i)));
            }
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            Set<String> keys = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                keys.add(entry.getKey());
            }
            for (String key : keys) {
                object.add(key, optimiseTable(object.get(key)));
            }
        }
        return element;
    }
}
