package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.RecipeHelper;
import me.shedaniel.rei.api.plugins.REIPluginV0;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.util.version.VersionParsingException;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.loot.LootTables;
import uk.me.desert_island.rer.RERUtils;

public class PluginEntry implements REIPluginV0 {
    public static final Identifier PLUGIN_ID = new Identifier("roughlyenoughresources", "rer_plugin");
    
    @Override public Identifier getPluginIdentifier() {
	return PLUGIN_ID;
    }
    
    @Override
    public SemanticVersion getMinimumVersion() throws VersionParsingException {
        return SemanticVersion.parse("3.0-pre");
    }
    
    @Override
    public void registerPluginCategories(RecipeHelper recipeHelper) {
        recipeHelper.registerCategory(new WorldGenCategory());
        recipeHelper.registerCategory(new LootCategory());
    }
    
    @Override
    public void registerRecipeDisplays(RecipeHelper recipeHelper) {
        System.out.printf("In registerRecipeDisplays\n");
        for (Block block : Registry.BLOCK) {
            //System.out.printf("block=%s\n", block);
            //System.out.printf("registerDisplay for WorldGenDisplay block=%s", block);

            {
                WorldGenRecipe r = new WorldGenRecipe(RERUtils.Block_to_ItemStack(block), block);
                recipeHelper.registerDisplay(WorldGenCategory.CATEGORY_ID, new WorldGenDisplay(r));
            }

            {
                Identifier drop_table_id = block.getDropTableId();
                
                if (drop_table_id != null &&
                    drop_table_id != LootTables.EMPTY) {
                        LootDisplay display = new BlockLootDisplay(block);
                        recipeHelper.registerDisplay(LootCategory.CATEGORY_ID, display);

                }
            }
        }

        for (EntityType<?> entity_type : Registry.ENTITY_TYPE) {
            Identifier drop_table_id = entity_type.getLootTableId();

            if (drop_table_id != null &&
                drop_table_id != LootTables.EMPTY) {
                LootDisplay display = new EntityLootDisplay(entity_type);
                recipeHelper.registerDisplay(LootCategory.CATEGORY_ID, display);

        }
}
    }
}
