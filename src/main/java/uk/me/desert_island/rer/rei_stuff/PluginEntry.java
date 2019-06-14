package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.REIPluginEntry;
import me.shedaniel.rei.api.RecipeHelper;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import uk.me.desert_island.rer.rei_stuff.WorldGenCategory;
import uk.me.desert_island.rer.rei_stuff.WorldGenDisplay;
import uk.me.desert_island.rer.rei_stuff.WorldGenRecipe;

public class PluginEntry implements REIPluginEntry {
    public static final Identifier PLUGIN_ID = new Identifier("roughlyenoughresources", "rer_plugin");
    
    @Override public Identifier getPluginIdentifier() {
	return PLUGIN_ID;
    }

    @Override
    public void registerPluginCategories(RecipeHelper recipeHelper) {
        recipeHelper.registerCategory(new WorldGenCategory());
    }

    @Override
    public void registerRecipeDisplays(RecipeHelper recipeHelper) {
        System.out.printf("In registerRecipeDisplays\n");
        for (Block block : Registry.BLOCK) {
            System.out.printf("block=%s\n", block);
            if (block.asItem() != null) {
                System.out.printf("registerDisplay for WorldGenDisplay block=%s", block);
                WorldGenRecipe r = new WorldGenRecipe(new ItemStack(block.asItem()));
                recipeHelper.registerDisplay(WorldGenCategory.CATEGORY_ID, new WorldGenDisplay(r));
            }
        }
    }
}
