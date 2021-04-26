package uk.me.desert_island.rer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import uk.me.desert_island.rer.client.ClientLootCache;
import uk.me.desert_island.rer.client.ClientWorldGenState;

import static uk.me.desert_island.rer.RoughlyEnoughResources.SEND_LOOT_INFO;
import static uk.me.desert_island.rer.RoughlyEnoughResources.SEND_WORLD_GEN_STATE;

@Environment(EnvType.CLIENT)
public class RoughlyEnoughResourcesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SEND_WORLD_GEN_STATE, (client, handler, buf, responseSender) -> {
            Identifier worldId = buf.readIdentifier();
            RegistryKey<World> world = RegistryKey.of(Registry.DIMENSION, worldId);
            if (world == null) {
                RERUtils.LOGGER.error("Found unregistered dimension type, does the server and client have the same dimensions? %s", worldId.toString());
                return;
            }
            ClientWorldGenState state = ClientWorldGenState.byWorld(world);
            state.readFromServerTag(buf);
            RERUtils.LOGGER.debug("Received data for " + worldId);
        });
        if (FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) {
            ClientPlayNetworking.registerGlobalReceiver(SEND_LOOT_INFO, (client, handler, buf, responseSender) -> {
                int size = buf.readInt();
                RERUtils.LOGGER.debug("Received %d Loot Info", size);
                for (int i = 0; i < size; i++) {
                    Identifier identifier = buf.readIdentifier();
                    String json = buf.readString(262144);
                    ClientLootCache.ID_TO_LOOT.put(identifier, json);
                }
            });
        }
    }
}
