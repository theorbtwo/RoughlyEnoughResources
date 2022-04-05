package uk.me.desert_island.rer.rei_stuff;

import dev.architectury.event.EventResult;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.client.ClientLootCache;
import uk.me.desert_island.rer.client.ClientWorldGenState;

import java.util.concurrent.atomic.AtomicLongArray;

import static uk.me.desert_island.rer.RoughlyEnoughResources.WORLD_HEIGHT;

@Environment(EnvType.CLIENT)
public class PluginEntry implements REIClientPlugin {
    public static final ResourceLocation PLUGIN_ID = new ResourceLocation("roughlyenoughresources", "rer_plugin");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        for (ResourceKey<Level> world : Minecraft.getInstance().getConnection().levels()) {
            registry.add(new WorldGenCategory(world));
        }
        registry.add(new LootCategory());
        registry.add(new EntityLootCategory());
        registry.removePlusButton(LootCategory.CATEGORY_ID);
        registry.removePlusButton(EntityLootCategory.CATEGORY_ID);
        for (ResourceKey<Level> world : Minecraft.getInstance().getConnection().levels()) {
            registry.removePlusButton(WorldGenCategory.WORLD_IDENTIFIER_MAP.get(world));
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (Block block : Registry.BLOCK) {
            for (ResourceKey<Level> world : Minecraft.getInstance().getConnection().levels()) {
                registry.add(new WorldGenDisplay(RERUtils.fromBlockToItemStackWithText(block), block, world));
            }

            ResourceLocation dropTableId = block.getLootTable();

            if (dropTableId != null && dropTableId != BuiltInLootTables.EMPTY) {
                registry.add(new BlockLootDisplay(block));
            }
        }

        for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
            ResourceLocation lootTableId = entityType.getDefaultLootTable();

            if (lootTableId != null && lootTableId != BuiltInLootTables.EMPTY) {
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
                for (int i = 0; i < WORLD_HEIGHT; i++) {
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
