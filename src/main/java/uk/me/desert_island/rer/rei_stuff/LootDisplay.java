package uk.me.desert_island.rer.rei_stuff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.shedaniel.rei.api.RecipeDisplay;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.loot.LootSupplier;
import net.minecraft.world.loot.context.LootContext;
import net.minecraft.world.loot.context.LootContext.Builder;
import net.minecraft.world.loot.context.LootContextType;

public abstract class LootDisplay implements RecipeDisplay<Recipe<Inventory>> {
	public ItemStack in_stack;
	public static ServerWorld world;
	public Identifier drop_table_id;
	public LootContextType context_type;
	public Map<ItemStack, Integer> outputs_map = null;

	@Override
	public Optional<Recipe<Inventory>> getRecipe() {
		return Optional.empty();
	}

	@Override
	public List<List<ItemStack>> getInput() {
		List<ItemStack> inner_list = new ArrayList<ItemStack>();
		inner_list.add(this.in_stack);
		List<List<ItemStack>> outer_stack = new ArrayList<List<ItemStack>>();
		outer_stack.add(inner_list);
		return new ArrayList<List<ItemStack>>();
	}

	@Override
	public List<ItemStack> getOutput() {
		Map<ItemStack, Integer> outputs_map = getOutputs();

		List<ItemStack> output_list = new ArrayList<ItemStack>();
		for (ItemStack item : outputs_map.keySet()) {
			output_list.add(item);
		}

		return output_list;
	}

	@Override
	public Identifier getRecipeCategory() {
		return LootCategory.CATEGORY_ID;
	}

	
	public Map<ItemStack, Integer> getOutputs() {
		if (world == null) {
			throw new Error("Don't know my world yet?");
		}
		if (outputs_map == null) {
			LootContext.Builder context_builder = new LootContext.Builder(world);
			this.fill_context_builder(context_builder);
			LootContext loot_context = context_builder.build(context_type);
			LootSupplier loot_supplier = loot_context.getLootManager().getSupplier(drop_table_id);

			outputs_map = new HashMap<ItemStack, Integer>();

			int samples = 100;

			for (int sample_i = 0; sample_i < samples; sample_i++) {
				List<ItemStack> sample = loot_supplier.getDrops(loot_context);
				for (ItemStack item : sample) {
					outputs_map.put(item, outputs_map.getOrDefault(item, 0) + 1);
				}
			}

		}
		return this.outputs_map;
	}

	abstract void fill_context_builder(Builder context_builder2);

	public void set_world(ServerWorld w) {
		world = w;
	}
}
