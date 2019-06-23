package uk.me.desert_island.rer.rei_stuff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import me.shedaniel.rei.api.RecipeDisplay;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import uk.me.desert_island.rer.rei_stuff.WorldGenRecipe;

public class WorldGenDisplay implements RecipeDisplay<WorldGenRecipe> {
	private WorldGenRecipe recipe;

	public WorldGenDisplay(WorldGenRecipe recipe) {
		this.recipe = recipe;
	}

	@Override
	public Optional<WorldGenRecipe> getRecipe() {
		return Optional.of(recipe);
	}

	@Override
	public List<List<ItemStack>> getInput() {
		return new ArrayList<List<ItemStack>>();
	}

	@Override
	public List<ItemStack> getOutput() {
		List<ItemStack> list = new ArrayList<ItemStack>(1);
		list.add(recipe.output_stack);
		return list;
	}

	@Override
	public Identifier getRecipeCategory() {
		return WorldGenCategory.CATEGORY_ID;
	}

	
}
