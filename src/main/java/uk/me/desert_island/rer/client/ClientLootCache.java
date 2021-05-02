package uk.me.desert_island.rer.client;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ClientLootCache {
    public static final Map<Identifier, String> ID_TO_LOOT = new ConcurrentHashMap<>();
}
