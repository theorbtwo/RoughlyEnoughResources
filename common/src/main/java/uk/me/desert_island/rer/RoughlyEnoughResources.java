package uk.me.desert_island.rer;

import com.google.common.collect.Lists;
import com.google.gson.*;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.GameInstance;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import uk.me.desert_island.rer.mixin.IdentifierHooks;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

public class RoughlyEnoughResources {
    public static final Gson GSON = Deserializers.createLootTableSerializer().create();

    public static final ResourceLocation SEND_WORLD_GEN_STATE_START = new ResourceLocation("roughlyenoughresources", "swds_start");
    public static final ResourceLocation SEND_WORLD_GEN_STATE_CHUNK = new ResourceLocation("roughlyenoughresources", "swds_chunk");
    public static final ResourceLocation SEND_WORLD_GEN_STATE_DONE = new ResourceLocation("roughlyenoughresources", "swds_done");
    public static final ResourceLocation SEND_LOOT_INFO = new ResourceLocation("roughlyenoughresources", "sli");
    public static final ResourceLocation ASK_SYNC_INFO = new ResourceLocation("roughlyenoughresources", "asi");

    public static final int MIN_WORLD_Y = -64;
    public static final int MAX_WORLD_Y = 320;
    public static final int WORLD_HEIGHT = MAX_WORLD_Y - MIN_WORLD_Y;

    public static void onInitialize() {
        RERUtils.LOGGER.info("RoughlyEnoughPacketSize?  Possibly.");
        NetworkManager.registerReceiver(NetworkManager.c2s(), ASK_SYNC_INFO, (buf, context) -> context.queue(() -> sendLootToPlayers(GameInstance.getServer(), Collections.singletonList((ServerPlayer) context.getPlayer()))));
    }

    public static void sendLootToPlayers(MinecraftServer server, List<ServerPlayer> players) {
        LootTables lootManager = server.getLootTables();
        List<ResourceLocation> names = Lists.newArrayList(lootManager.getIds());

        int size = 50;
        for (int i = 0; i < names.size(); i += size) {
            int end = Math.min(names.size(), i + size);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(end - i);
            for (int j = i; j < end; j++) {
                ResourceLocation identifier = names.get(j);
                LootTable table = lootManager.get(identifier);
                writeIdentifier(buf, identifier);
                writeJson(buf, optimiseTable(GSON.toJsonTree(table)));
            }
            for (ServerPlayer player : players) {
                NetworkManager.sendToPlayer(player, RoughlyEnoughResources.SEND_LOOT_INFO, new FriendlyByteBuf(buf.duplicate()));
            }
        }
    }

    private static JsonElement optimiseTable(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isString()) {
                String s = element.getAsJsonPrimitive().getAsString();
                if (s.length() >= 11 && s.startsWith("minecraft:")) {
                    String substring = s.substring(10);
                    if (IdentifierHooks.isPathValid(substring))
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

    public static void writeIdentifier(FriendlyByteBuf buf, ResourceLocation identifier) {
        if (identifier.getNamespace().equals("minecraft")) {
            buf.writeUtf(identifier.getPath());
        } else {
            buf.writeUtf(identifier.toString());
        }
    }

    // NULL 0
    // STR 1
    // FALSE 2
    // TRUE 3
    // INT 4
    // LONG 5
    // SHORT 6
    // BYTE 7
    // BIG_INT 8
    // FLOAT 9
    // DOUBLE 10
    // BIG_DECIMAL 11
    // ARRAY 12
    // OBJECT 13
    public static void writeJson(FriendlyByteBuf buf, JsonElement element) {
        if (element.isJsonNull()) {
            buf.writeByte(0);
        } else if (element.isJsonPrimitive()) {
            writeJsonPrimitive(buf, element.getAsJsonPrimitive());
        } else if (element.isJsonArray()) {
            buf.writeByte(12);
            JsonArray array = element.getAsJsonArray();
            buf.writeVarInt(array.size());
            for (JsonElement arrayElement : array) {
                writeJson(buf, arrayElement);
            }
        } else if (element.isJsonObject()) {
            buf.writeByte(13);
            JsonObject object = element.getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> entrySet = object.entrySet();
            buf.writeVarInt(entrySet.size());
            for (Map.Entry<String, JsonElement> entry : entrySet) {
                buf.writeUtf(entry.getKey());
                writeJson(buf, entry.getValue());
            }
        } else {
            throw new IllegalArgumentException("Unknown json element type: " + element.getClass());
        }
    }

    private static void writeJsonPrimitive(FriendlyByteBuf buf, JsonPrimitive primitive) {
        if (primitive.isString()) {
            buf.writeByte(1);
            buf.writeUtf(primitive.getAsString(), Integer.MAX_VALUE);
        } else if (primitive.isBoolean()) {
            buf.writeByte(primitive.getAsBoolean() ? 3 : 2);
        } else if (primitive.isNumber()) {
            Number number = primitive.getAsNumber();
            if (number instanceof Integer) {
                buf.writeByte(4);
                buf.writeVarInt(number.intValue());
            } else if (number instanceof Long) {
                buf.writeByte(5);
                buf.writeVarLong(number.longValue());
            } else if (number instanceof Short) {
                buf.writeByte(6);
                buf.writeShort(number.shortValue());
            } else if (number instanceof Byte) {
                buf.writeByte(7);
                buf.writeByte(number.byteValue());
            } else if (number instanceof BigInteger) {
                buf.writeByte(8);
                byte[] bytes = ((BigInteger) number).toByteArray();
                buf.writeByteArray(bytes);
            } else if (number instanceof Float) {
                buf.writeByte(9);
                buf.writeFloat(number.floatValue());
            } else if (number instanceof Double) {
                buf.writeByte(10);
                buf.writeDouble(number.doubleValue());
            } else if (number instanceof BigDecimal) {
                buf.writeByte(11);
                // serialize with unscaled value, scale, and precision
                BigDecimal decimal = (BigDecimal) number;
                buf.writeByteArray(decimal.unscaledValue().toByteArray());
                buf.writeInt(decimal.scale());
                buf.writeInt(decimal.precision());
            } else {
                throw new IllegalArgumentException("Unknown number type: " + number.getClass());
            }
        } else {
            throw new IllegalArgumentException("Unknown primitive type: " + primitive.getClass());
        }
    }

    public static JsonElement readJson(FriendlyByteBuf buf) {
        byte type = buf.readByte();
        switch (type) {
            case 0:
                return JsonNull.INSTANCE;
            case 12:
                int size = buf.readVarInt();
                JsonArray array = new JsonArray(size);
                for (int i = 0; i < size; i++) {
                    array.add(readJson(buf));
                }
                return array;
            case 13:
                size = buf.readVarInt();
                JsonObject object = new JsonObject();
                for (int i = 0; i < size; i++) {
                    String key = buf.readUtf();
                    object.add(key, readJson(buf));
                }
                return object;
            default:
                if (type < 1 || type > 11) {
                    throw new IllegalArgumentException("Unknown json type: " + type);
                }
                return readJsonPrimitive(type, buf);
        }
    }

    private static JsonPrimitive readJsonPrimitive(int type, FriendlyByteBuf buf) {
        switch (type) {
            case 1:
                return new JsonPrimitive(buf.readUtf(Integer.MAX_VALUE / 4));
            case 2:
                return new JsonPrimitive(false);
            case 3:
                return new JsonPrimitive(true);
            case 4:
                return new JsonPrimitive(buf.readVarInt());
            case 5:
                return new JsonPrimitive(buf.readVarLong());
            case 6:
                return new JsonPrimitive(buf.readShort());
            case 7:
                return new JsonPrimitive(buf.readByte());
            case 8:
                return new JsonPrimitive(new BigInteger(buf.readByteArray()));
            case 9:
                return new JsonPrimitive(buf.readFloat());
            case 10:
                return new JsonPrimitive(buf.readDouble());
            case 11:
                // deserialize with unscaled value, scale, and precision
                byte[] unscaledValue = buf.readByteArray();
                int scale = buf.readInt();
                int precision = buf.readInt();
                MathContext context = new MathContext(precision);
                return new JsonPrimitive(new BigDecimal(new BigInteger(unscaledValue), scale, context));
            default:
                throw new IllegalArgumentException("Unknown json primitive type: " + type);
        }
    }
}
