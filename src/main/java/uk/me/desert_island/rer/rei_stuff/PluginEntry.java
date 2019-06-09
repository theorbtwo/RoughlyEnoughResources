package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.*;
import net.minecraft.util.Identifier;


public class PluginEntry implements REIPluginEntry {
    public static final Identifier PLUGIN_ID = new Identifier("roughlyenoughresources", "rer_plugin");
    
    @Override
    public Identifier getPluginIdentifier() {
	return PLUGIN_ID;
    }

    
    @Override
    public void registerPluginCategories(RecipeHelper recipeHelper) {
	recipeHelper.registerCategory(new WorldGenCategory());
    }
}
