package uk.me.desert_island.rer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import uk.me.desert_island.rer.client.ClientLootCache;
import uk.me.desert_island.rer.client.ClientWorldGenState;
import net.minecraft.village.VillagerProfession;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;

import static uk.me.desert_island.rer.RoughlyEnoughResources.SEND_LOOT_INFO;
import static uk.me.desert_island.rer.RoughlyEnoughResources.SEND_WORLD_GEN_STATE_START;
import static uk.me.desert_island.rer.RoughlyEnoughResources.SEND_WORLD_GEN_STATE_CHUNK;
import static uk.me.desert_island.rer.RoughlyEnoughResources.SEND_WORLD_GEN_STATE_DONE;

import static io.netty.buffer.Unpooled.*;

import java.util.Random;

import io.netty.buffer.CompositeByteBuf;

import static net.minecraft.village.TradeOffers.PROFESSION_TO_LEVELED_TRADE;

@Environment(EnvType.CLIENT)
public class RoughlyEnoughResourcesClient implements ClientModInitializer {
    private CompositeByteBuf world_state_buf;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SEND_WORLD_GEN_STATE_START, (client, handler, this_buf, responseSender) -> {
            // int size = this_buf.readInt();
            RERUtils.LOGGER.debug("got SEND_WORLD_GEN_STATE_START");
            world_state_buf = compositeBuffer();
        });

        ClientPlayNetworking.registerGlobalReceiver(SEND_WORLD_GEN_STATE_CHUNK, (client, handler, this_buf, responseSender) -> {
            RERUtils.LOGGER.debug("got SEND_WORLD_GEN_STATE_CHUNK");
            world_state_buf.addComponent(true, this_buf.retainedDuplicate());
        });

        ClientPlayNetworking.registerGlobalReceiver(SEND_WORLD_GEN_STATE_DONE, (client, handler, this_buf, responseSender) -> {
            RERUtils.LOGGER.debug("got SEND_WORLD_GEN_STATE_DONE");
            PacketByteBuf buf = new PacketByteBuf(world_state_buf);
            Identifier worldId = buf.readIdentifier();
            RegistryKey<World> world = RegistryKey.of(Registry.DIMENSION, worldId);
            if (world == null) {
                RERUtils.LOGGER.error("Found unregistered dimension type %s, do the server and client have the same dimensions?", worldId.toString());
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
