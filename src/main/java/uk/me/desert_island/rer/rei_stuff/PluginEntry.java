package uk.me.desert_island.rer.rei_stuff;

import dev.architectury.event.EventResult;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootTables;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.client.ClientLootCache;
import uk.me.desert_island.rer.client.ClientWorldGenState;

import java.util.concurrent.atomic.AtomicLongArray;

@Environment(EnvType.CLIENT)
public class PluginEntry implements REIClientPlugin {
    public static final Identifier PLUGIN_ID = new Identifier("roughlyenoughresources", "rer_plugin");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        for (RegistryKey<World> world : MinecraftClient.getInstance().getNetworkHandler().getWorldKeys()) {
            registry.add(new WorldGenCategory(world));
        }
        registry.add(new LootCategory());
        registry.add(new EntityLootCategory());
        registry.removePlusButton(LootCategory.CATEGORY_ID);
        registry.removePlusButton(EntityLootCategory.CATEGORY_ID);
        for (RegistryKey<World> world : MinecraftClient.getInstance().getNetworkHandler().getWorldKeys()) {
            registry.removePlusButton(WorldGenCategory.WORLD_IDENTIFIER_MAP.get(world));
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (Block block : Registry.BLOCK) {
            for (RegistryKey<World> world : MinecraftClient.getInstance().getNetworkHandler().getWorldKeys()) {
                registry.add(new WorldGenDisplay(RERUtils.fromBlockToItemStackWithText(block), block, world));
            }

            Identifier dropTableId = block.getLootTableId();

            if (dropTableId != null && dropTableId != LootTables.EMPTY) {
                registry.add(new BlockLootDisplay(block));
            }
        }

        for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
            Identifier lootTableId = entityType.getLootTableId();

            if (lootTableId != null && lootTableId != LootTables.EMPTY) {
                registry.add(new EntityLootDisplay(entityType));
            }
        }

        registry.registerVisibilityPredicate((category, display) -> {
            if (display instanceof WorldGenDisplay) {
                WorldGenDisplay worldGenDisplay = (WorldGenDisplay) display;
                WorldGenCategory worldGenCategory = (WorldGenCategory) category;
                ClientWorldGenState state = ClientWorldGenState.byWorld(worldGenCategory.getWorld());
                AtomicLongArray levelCount = state.levelCountsMap.get(worldGenDisplay.getOutputBlock());
                if (levelCount == null)
                    return EventResult.interruptFalse();
                for (int i = 0; i < 128; i++) {
                    if (levelCount.get(i) > 0)
                        return EventResult.pass();
                }
                return EventResult.interruptFalse();
            }
            if (display instanceof LootDisplay) {
                if (!ClientLootCache.ID_TO_LOOT.containsKey(((LootDisplay) display).lootTableId) || ((LootDisplay) display).getOutputs().isEmpty())
                    return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
    }
}
