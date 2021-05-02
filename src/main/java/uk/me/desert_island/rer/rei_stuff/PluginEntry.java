package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.RecipeHelper;
import me.shedaniel.rei.api.plugins.REIPluginV0;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootTables;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.client.ClientLootCache;
import uk.me.desert_island.rer.client.ClientWorldGenState;

import java.util.concurrent.atomic.AtomicLongArray;

@Environment(EnvType.CLIENT)
public class PluginEntry implements REIPluginV0 {
    public static final Identifier PLUGIN_ID = new Identifier("roughlyenoughresources", "rer_plugin");

    @Override
    public Identifier getPluginIdentifier() {
        return PLUGIN_ID;
    }

    @Override
    public void registerPluginCategories(RecipeHelper recipeHelper) {
        for (RegistryKey<World> world : MinecraftClient.getInstance().getNetworkHandler().getWorldKeys()) {
            recipeHelper.registerCategory(new WorldGenCategory(world));
        }
        recipeHelper.registerCategory(new LootCategory());
        recipeHelper.registerCategory(new EntityLootCategory());
    }

    @Override
    public void registerRecipeDisplays(RecipeHelper recipeHelper) {
        for (Block block : Registry.BLOCK) {
            for (RegistryKey<World> world : MinecraftClient.getInstance().getNetworkHandler().getWorldKeys()) {
                recipeHelper.registerDisplay(new WorldGenDisplay(RERUtils.fromBlockToItemStackWithText(block), block, world));
            }

            Identifier dropTableId = block.getLootTableId();

            if (dropTableId != null && dropTableId != LootTables.EMPTY) {
                recipeHelper.registerDisplay(new BlockLootDisplay(block));
            }
        }

        for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
            Identifier lootTableId = entityType.getLootTableId();

            if (lootTableId != null && lootTableId != LootTables.EMPTY) {
                recipeHelper.registerDisplay(new EntityLootDisplay(entityType));
            }
        }
    }

    @Override
    public void registerOthers(RecipeHelper recipeHelper) {
        recipeHelper.removeAutoCraftButton(LootCategory.CATEGORY_ID);
        recipeHelper.removeAutoCraftButton(EntityLootCategory.CATEGORY_ID);
        for (RegistryKey<World> world : MinecraftClient.getInstance().getNetworkHandler().getWorldKeys()) {
            recipeHelper.removeAutoCraftButton(WorldGenCategory.WORLD_IDENTIFIER_MAP.get(world));
        }
        recipeHelper.registerRecipeVisibilityHandler((category, display) -> {
            if (display instanceof WorldGenDisplay) {
                WorldGenDisplay worldGenDisplay = (WorldGenDisplay) display;
                WorldGenCategory worldGenCategory = (WorldGenCategory) category;
                ClientWorldGenState state = ClientWorldGenState.byWorld(worldGenCategory.getWorld());
                AtomicLongArray levelCount = state.levelCountsMap.get(worldGenDisplay.getOutputBlock());
                if (levelCount == null)
                    return ActionResult.FAIL;
                for (int i = 0; i < 128; i++) {
                    if (levelCount.get(i) > 0)
                        return ActionResult.SUCCESS;
                }
                return ActionResult.FAIL;
            }
            if (display instanceof LootDisplay) {
                if (!ClientLootCache.ID_TO_LOOT.containsKey(((LootDisplay) display).lootTableId) || ((LootDisplay) display).getOutputs().isEmpty())
                    return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
