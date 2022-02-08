package uk.me.desert_island.rer.client;

import com.google.gson.JsonElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ClientLootCache {
    public static final Map<Identifier, JsonElement> ID_TO_LOOT = new ConcurrentHashMap<>();
}
