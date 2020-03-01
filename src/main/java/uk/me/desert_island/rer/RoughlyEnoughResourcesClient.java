package uk.me.desert_island.rer;

import me.shedaniel.math.api.Executor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import uk.me.desert_island.rer.client.ClientLootCache;
import uk.me.desert_island.rer.client.ClientWorldGenState;

import static uk.me.desert_island.rer.RoughlyEnoughResources.SEND_LOOT_INFO;
import static uk.me.desert_island.rer.RoughlyEnoughResources.SEND_WORLD_GEN_STATE;

@Environment(EnvType.CLIENT)
public class RoughlyEnoughResourcesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientSidePacketRegistry.INSTANCE.register(SEND_WORLD_GEN_STATE, (packetContext, packetByteBuf) -> {
            Identifier dimensionTypeId = packetByteBuf.readIdentifier();
            DimensionType dimensionType = Registry.DIMENSION_TYPE.get(dimensionTypeId);
            if (dimensionType == null) {
                RERUtils.LOGGER.error("Found unregistered dimension type, does the server and client have the same dimensions? %s", dimensionTypeId.toString());
                return;
            }
            ClientWorldGenState state = ClientWorldGenState.byDimension(dimensionType);
            state.readFromServerTag(packetByteBuf.readCompoundTag());
            RERUtils.LOGGER.debug("Received data for " + dimensionTypeId);
        });
        if (FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) {
            Executor.run(() -> () -> {
                ClientSidePacketRegistry.INSTANCE.register(SEND_LOOT_INFO, (packetContext, packetByteBuf) -> {
                    int size = packetByteBuf.readInt();
                    RERUtils.LOGGER.debug("Received %d Loot Info", size);
                    for (int i = 0; i < size; i++) {
                        Identifier identifier = packetByteBuf.readIdentifier();
                        String json = packetByteBuf.readString();
                        ClientLootCache.ID_TO_LOOT.put(identifier, json);
                    }
                });
            });
        }
    }
}
