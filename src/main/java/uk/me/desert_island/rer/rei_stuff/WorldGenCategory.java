package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.*;
import net.minecraft.util.Identifier;


public class WorldGenCategory implements RecipeCategory<WorldGenDisplay> {
    public static final Identifier CATEGORY_ID = new Identifier("roughlyenoughresources", "worldgen_category");
    
    @Override
    public Identifier getIdentifier() {
	return CATEGORY_ID;
    }

    @Override
    public String getCategoryName() {
	return I18n.translate("category.rer.worldgen");
    }
    
}
