package uk.me.desert_island.rer.rei_stuff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.shedaniel.rei.api.RecipeDisplay;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BoundedIntUnaryOperator;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.loot.BinomialLootTableRange;
import net.minecraft.world.loot.ConstantLootTableRange;
import net.minecraft.world.loot.LootPool;
import net.minecraft.world.loot.LootSupplier;
import net.minecraft.world.loot.UniformLootTableRange;
import net.minecraft.world.loot.condition.LootCondition;
import net.minecraft.world.loot.condition.LootConditions;
import net.minecraft.world.loot.context.LootContext;
import net.minecraft.world.loot.context.LootContext.Builder;
import net.minecraft.world.loot.context.LootContextType;
import net.minecraft.world.loot.entry.LootEntries;
import net.minecraft.world.loot.entry.LootEntry;
import net.minecraft.world.loot.function.LootFunction;
import net.minecraft.world.loot.function.LootFunctions;
import uk.me.desert_island.rer.LootOutput;

public abstract class LootDisplay implements RecipeDisplay<Recipe<Inventory>> {
	public ItemStack in_stack;
	public static ServerWorld world;
	public Identifier drop_table_id;
	public LootContextType context_type;
	public List<LootOutput> outputs = null;

	/* from LootManager */
	private static final Gson gson = (new GsonBuilder())
		.registerTypeAdapter(UniformLootTableRange.class, new UniformLootTableRange.Serializer())
		.registerTypeAdapter(BinomialLootTableRange.class, new BinomialLootTableRange.Serializer())
		.registerTypeAdapter(ConstantLootTableRange.class, new ConstantLootTableRange.Serializer())
		.registerTypeAdapter(BoundedIntUnaryOperator.class, new BoundedIntUnaryOperator.Serializer())
		.registerTypeAdapter(LootPool.class, new LootPool.Serializer())
		.registerTypeAdapter(LootSupplier.class, new LootSupplier.Serializer())
		.registerTypeHierarchyAdapter(LootEntry.class, new LootEntries.Serializer())
		.registerTypeHierarchyAdapter(LootFunction.class, new LootFunctions.Factory())
		.registerTypeHierarchyAdapter(LootCondition.class, new LootConditions.Factory())
		.registerTypeHierarchyAdapter(LootContext.EntityTarget.class, new LootContext.EntityTarget.Serializer())
		.create();

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
		List<LootOutput> outputs = getOutputs();

		List<ItemStack> output_stacks = new ArrayList<ItemStack>();
		for (LootOutput output : outputs) {
			output_stacks.add(output.output);
		}

		return output_stacks;
	}

	@Override
	public Identifier getRecipeCategory() {
		return LootCategory.CATEGORY_ID;
	}

	public List<LootOutput> munch_loot_entry_alternatives_json(JsonObject json_entry) {
		List<LootOutput> outputs = new ArrayList<LootOutput>();
		JsonArray children = json_entry.get("children").getAsJsonArray();

		for (JsonElement child : children) {
			outputs.addAll(munch_loot_entry_json(child.getAsJsonObject()));
		}

		return outputs;
	}

	public List<LootOutput> munch_loot_entry_item_json(JsonObject json_entry) {
		Item item = Registry.ITEM.get(new Identifier(json_entry.get("name").getAsString()));
		ItemStack item_stack = new ItemStack(item);
		LootOutput output = new LootOutput();
		output.output = item_stack;
		List<LootOutput> outputs = new ArrayList<LootOutput>();
		outputs.add(output);
		return outputs;
	}

	public void munch_loot_condition(JsonElement cond_elem, List<LootOutput> outputs) {
		JsonObject cond_obj = cond_elem.getAsJsonObject();
		String kind = cond_obj.get("condition").getAsString();

		if (kind.equals("minecraft:survives_explosion")) {
			/* Do nothing, this is generally just confusing. */
		} else if (kind.equals("minecraft:match_tool") &&
		           cond_obj.get("predicate").getAsJsonObject().has("enchantments"))
		{
			for (JsonElement ench_elem : cond_obj.get("predicate").getAsJsonObject().get("enchantments").getAsJsonArray()) {
				JsonObject ench_obj = ench_elem.getAsJsonObject();
				String enchantment = ench_obj.get("enchantment").getAsString();
				if (enchantment.equals("minecraft:silk_touch")) {
					for (LootOutput output : outputs) {
						output.add_extra_text("silk touch");
					}
				} else {
					System.out.printf("Unhandled enchantment %s\n", enchantment);
				}
			}
		} else {
			//throw new Error(String.format("Don't know how to deal with condition of type %s (%s)", kind, cond_obj));
			System.out.printf("Don't know how to deal with condition of type %s (%s)", kind, cond_obj);
		}

	}

	public List<LootOutput> munch_loot_entry_json(JsonObject json_entry) {
		String type = json_entry.get("type").getAsString();

		List<LootOutput> outputs = new ArrayList<LootOutput>();
		/* creeper_spawn_egg -> minecraft:tag */
		/* elder_guardian_spawn_egg -> minecraft:loot_table */
		if (type.equals("minecraft:item")) {
			outputs.addAll(munch_loot_entry_item_json(json_entry));
		} else if (type.equals("minecraft:alternatives")) {
			outputs.addAll(munch_loot_entry_alternatives_json(json_entry));
		} else if (type.equals("minecraft:empty")) {
			/* elder_guardian_spawn_egg, guardian_spawn_egg */
			/* do nothing */
		} else {
			//throw new Error(String.format("Don't know how to deal with entry of type %s (%s)", type, json_entry));
			System.out.printf("Don't know how to deal with entry of type %s (%s)", type, json_entry);
		}

		if (json_entry.has("conditions")) {
			for (JsonElement cond_elem : json_entry.get("conditions").getAsJsonArray()) {
				munch_loot_condition(cond_elem, outputs);
			}
		}

		return outputs;
	}

	public List<LootOutput> munch_loot_pool_json(JsonObject json_pool) {
		List<LootOutput> outputs = new ArrayList<LootOutput>();

		JsonArray entries = json_pool.getAsJsonObject().get("entries").getAsJsonArray();
		for (JsonElement pool_element : entries) {
			JsonObject entry_object = pool_element.getAsJsonObject();

			outputs.addAll(munch_loot_entry_json(entry_object));
		}


		return outputs;
	}

	public List<LootOutput> munch_loot_supplier_json(JsonElement json_supplier) {
		List<LootOutput> outputs = new ArrayList<LootOutput>();

		if (!json_supplier.getAsJsonObject().has("pools")) {
			return outputs;
		}

		JsonArray pools = json_supplier.getAsJsonObject().get("pools").getAsJsonArray();
		for (JsonElement pool_element : pools) {
			JsonObject pool_object = pool_element.getAsJsonObject();

			outputs.addAll(munch_loot_pool_json(pool_object));
		}

		return outputs;
	}

	public List<LootOutput> getOutputs() {
		if (world == null) {
			throw new Error("Don't know my world yet?");
		}
		if (outputs == null) {
			LootContext.Builder context_builder = new LootContext.Builder(world);
			if (!this.fill_context_builder(context_builder, world)) {
				return new ArrayList<LootOutput>();
			}
			LootContext loot_context = context_builder.build(context_type);
			LootSupplier loot_supplier = loot_context.getLootManager().getSupplier(drop_table_id);

			JsonElement json_supplier = gson.toJsonTree(loot_supplier);

			System.out.printf("\n");
			System.out.printf("input %s\n", this.in_stack);
			System.out.printf("json: %s\n", json_supplier);

			outputs = munch_loot_supplier_json(json_supplier);

			for (LootOutput out : outputs) {
				System.out.println(out);
			}
			

		}
		return outputs;
	}

	abstract boolean fill_context_builder(Builder context_builder2, World world);

	public void set_world(ServerWorld w) {
		world = w;
	}
}
