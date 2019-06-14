package uk.me.desert_island.rer.rei_stuff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.shedaniel.rei.api.RecipeDisplay;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import uk.me.desert_island.rer.rei_stuff.WorldGenRecipe;

public class WorldGenDisplay implements RecipeDisplay<WorldGenRecipe> {
	private WorldGenRecipe recipe;
	private Map<Integer, Double> prob_map;
	private int min_level = 9999;
	private int max_level = 0;

	public WorldGenDisplay(WorldGenRecipe recipe) {
		this.recipe = recipe;

		return;

		//Map<Integer, Map<Block, Integer>> block_counts_at_level = null /* FIXME! */;
		//Map<Integer, Integer> total_counts_at_level = null /* FIXME! */;

		/*for (int y : total_counts_at_level.keySet()) {
			Map<Block, Integer> block_counts_at_this_level = block_counts_at_level.get(y);
			double prob;

			if (block_counts_at_this_level.containsKey(output_block)) {
				prob = (double) block_counts_at_this_level.get(output_block) / total_counts_at_level.get(y);
			} else {
				prob = 0;
			}

			prob_map.put(y, prob);

			if (prob > 0) {
				if (y < min_level) {
					min_level = y;
				}
				if (y > max_level) {
					max_level = y;
				}
			}
		}*/
	}

	@Override
	public Optional<WorldGenRecipe> getRecipe() {
		return Optional.of(recipe);
	}

	@Override
	public List<List<ItemStack>> getInput() {
		return null;
	}

	@Override
	public List<ItemStack> getOutput() {
		List<ItemStack> list = new ArrayList<ItemStack>(1);
		list.add(recipe.output);
		return list;
	}

	@Override
	public Identifier getRecipeCategory() {
		return null;
	}
}
