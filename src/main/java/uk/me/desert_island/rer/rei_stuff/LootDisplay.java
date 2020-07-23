package uk.me.desert_island.rer.rei_stuff;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.api.RecipeDisplay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.RoughlyEnoughResources;
import uk.me.desert_island.rer.client.ClientLootCache;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("StatementWithEmptyBody")
@Environment(EnvType.CLIENT)
public abstract class LootDisplay implements RecipeDisplay {
    public EntryStack inputStack;
    public Identifier lootTableId;
    public LootContextType contextType;
    public List<LootOutput> outputs = null;
    public static final NumberFormat FORMAT = new DecimalFormat("#.##");
    public static final NumberFormat FORMAT_MORE = new DecimalFormat("#.####");

    @Override
    public List<List<EntryStack>> getInputEntries() {
        return Collections.singletonList(Collections.singletonList(this.inputStack));
    }

    public abstract Identifier getLocation();

    @Override
    public List<EntryStack> getOutputEntries() {
        List<EntryStack> stacks = Lists.newArrayList();
        for (LootOutput output : getOutputs()) {
            stacks.add(output.original);
        }
        return stacks;
    }

    public List<EntryStack> getFlattenedOutputEntries() {
        List<EntryStack> stacks = Lists.newArrayList();
        for (List<EntryStack> entry : getFullOutputEntries()) {
            if (entry.isEmpty()) {
                stacks.add(EntryStack.empty());
            } else {
                stacks.add(entry.get(0));
            }
        }
        return stacks;
    }

    public List<List<EntryStack>> getFullOutputEntries() {
        List<List<EntryStack>> stacks = Lists.newArrayList();
        for (LootOutput output : getOutputs()) {
            stacks.add(output.output);
        }
        return stacks;
    }

    @Override
    public Identifier getRecipeCategory() {
        return LootCategory.CATEGORY_ID;
    }

    public List<LootOutput> munchLootEntryAlternativesJson(JsonObject object) {
        List<LootOutput> outputs = new ArrayList<>();
        JsonArray children = object.get("children").getAsJsonArray();

        for (JsonElement child : children) {
            outputs.addAll(munchLootEntryJson(child.getAsJsonObject()));
        }

        if (object.getAsJsonObject().has("conditions")) {
            for (JsonElement conditionElement : object.getAsJsonObject().get("conditions").getAsJsonArray()) {
                munchLootCondition(conditionElement, outputs);
            }
        }
        if (object.getAsJsonObject().has("functions")) {
            List<LootOutput> newOutputs = new ArrayList<>();
            for (JsonElement functionElement : object.getAsJsonObject().get("functions").getAsJsonArray()) {
                List<LootOutput> list = munchLootFunctions(functionElement, outputs);
                if (list != null)
                    newOutputs.addAll(list);
            }
            outputs.addAll(newOutputs);
        }

        return outputs;
    }

    public List<LootOutput> munchLootEntryItemJson(JsonObject object) {
        Item item = Registry.ITEM.get(new Identifier(object.get("name").getAsString()));
        EntryStack stack = EntryStack.create(item);
        LootOutput output = new LootOutput();
        output.output = Lists.newArrayList(stack);
        output.original = stack.copy();
        List<LootOutput> outputs = new ArrayList<>();
        outputs.add(output);
        return outputs;
    }

    public void munchLootCondition(JsonElement conditionElement, List<LootOutput> outputs) {
        JsonObject conditionObject = conditionElement.getAsJsonObject();
        String kind = new Identifier(conditionObject.get("condition").getAsString()).toString();

        if (kind.equals("minecraft:inverted")) {
            for (LootOutput output : outputs) {
                output.nowInverted = !output.nowInverted;
                output.lastInverted = false;
            }
            if (conditionObject.has("term"))
                munchLootCondition(conditionObject.get("term").getAsJsonObject(), outputs);
            if (conditionObject.has("terms"))
                for (JsonElement alternateCondition : conditionObject.get("terms").getAsJsonArray()) {
                    munchLootCondition(alternateCondition, outputs);
                }
            for (LootOutput output : outputs) {
                output.nowInverted = !output.nowInverted;
                output.lastInverted = false;
            }
        } else if (kind.equals("minecraft:alternative")) {
            if (conditionObject.has("term"))
                munchLootCondition(conditionObject.get("term").getAsJsonObject(), outputs);
            if (conditionObject.has("terms"))
                for (JsonElement alternateCondition : conditionObject.get("terms").getAsJsonArray()) {
                    munchLootCondition(alternateCondition, outputs);
                }
        } else if (kind.equals("minecraft:killed_by_player")) {
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.translate("rer.condition.killedByPlayer"));
            }
        } else if (kind.equals("minecraft:survives_explosion")) {
            /* Do nothing, this is generally just confusing. */
        } else if (kind.equals("minecraft:entity_properties") && conditionObject.has("predicate") && conditionObject.get("predicate").getAsJsonObject().has("flags") && conditionObject.get("predicate").getAsJsonObject().get("flags").getAsJsonObject().has("is_on_fire") && conditionObject.get("predicate").getAsJsonObject().get("flags").getAsJsonObject().get("is_on_fire").getAsBoolean() && conditionObject.has("entity") && conditionObject.get("entity").getAsString().equals("this")) {
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.translate("rer.condition.onFire"));
            }
        } else if (kind.equals("minecraft:block_state_property") || kind.equals("minecraft:entity_properties") || kind.equals("minecraft:damage_source_properties")) {
            // ignore
        } else if (kind.equals("minecraft:match_tool") && conditionObject.has("predicate") && conditionObject.get("predicate").getAsJsonObject().has("enchantments")) {
            for (JsonElement enchantmentElement : conditionObject.get("predicate").getAsJsonObject().get("enchantments").getAsJsonArray()) {
                JsonObject enchantmentObject = enchantmentElement.getAsJsonObject();
                String enchantmentString = enchantmentObject.get("enchantment").getAsString();
                Enchantment enchantment = Registry.ENCHANTMENT.get(new Identifier(enchantmentString));
                if (enchantment != null) {
                    for (LootOutput output : outputs) {
                        output.addExtraText(I18n.translate("rer.condition.enchantment", I18n.translate(enchantment.getTranslationKey()).toLowerCase()));
                    }
                }
            }
        } else if (kind.equals("minecraft:random_chance") && conditionObject.has("chance")) {
            double chance = conditionObject.get("chance").getAsDouble() * 100.0;
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.translate("rer.condition.chance", FORMAT.format(chance)));
            }
        } else if (kind.equals("minecraft:random_chance_with_looting") && conditionObject.has("chance")) {
            double chance = conditionObject.get("chance").getAsDouble() * 100.0;
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.translate("rer.condition.chance.looting", FORMAT.format(chance)));
            }
        } else if (kind.equals("minecraft:match_tool") && conditionObject.has("predicate") && conditionObject.get("predicate").getAsJsonObject().has("item")) {
            String itemId = conditionObject.get("predicate").getAsJsonObject().get("item").getAsString();
            Item item = Registry.ITEM.get(new Identifier(itemId));
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.translate("rer.condition.item", item.getName().getString().toLowerCase()));
            }
        } else if (kind.equals("minecraft:table_bonus")) {
            double chance = conditionObject.get("chances").getAsJsonArray().get(0).getAsDouble() * 100.0;
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.translate("rer.condition.chance", FORMAT.format(chance)));
            }
        } else {
            RERUtils.LOGGER.debug("Don't know how to deal with condition of type %s (%s)", kind, conditionObject);
        }
    }

    public List<LootOutput> munchLootEntryJson(JsonObject object) {
        String type = new Identifier(object.get("type").getAsString()).toString();

        List<LootOutput> outputs = new ArrayList<>();
        /* creeper_spawn_egg -> minecraft:tag */
        /* elder_guardian_spawn_egg -> minecraft:loot_table */
        switch (type) {
            case "minecraft:item":
                outputs.addAll(munchLootEntryItemJson(object));
                break;
            case "minecraft:alternatives":
                outputs.addAll(munchLootEntryAlternativesJson(object));
                break;
            case "minecraft:empty":
                /* do nothing */
                break;
            case "minecraft:loot_table":
                String json = ClientLootCache.ID_TO_LOOT.get(new Identifier(object.get("name").getAsString()));
                if (json != null)
                    outputs.addAll(munchLootSupplierJson(RoughlyEnoughResources.GSON.fromJson(json, JsonElement.class)));
                break;
            case "minecraft:tag":
                Tag<Item> tag = ItemTags.getContainer().get(new Identifier(object.get("name").getAsString()));
                if (tag != null)
                    outputs.addAll(tag.values().stream().map(item -> {
                        EntryStack stack = EntryStack.create(item);
                        LootOutput output = new LootOutput();
                        output.output = Lists.newArrayList(stack);
                        output.original = stack.copy();
                        return output;
                    }).collect(Collectors.toList()));
                break;
            default:
                RERUtils.LOGGER.debug("Don't know how to deal with entry of type %s (%s)", type, object);
                break;
        }

        if (object.has("conditions")) {
            for (JsonElement conditionElement : object.get("conditions").getAsJsonArray()) {
                munchLootCondition(conditionElement, outputs);
            }
        }

        if (object.has("functions")) {
            List<LootOutput> newOutputs = new ArrayList<>();
            for (JsonElement functionElement : object.get("functions").getAsJsonArray()) {
                List<LootOutput> list = munchLootFunctions(functionElement, outputs);
                if (list != null)
                    newOutputs.addAll(list);
            }
            outputs.addAll(newOutputs);
        }

        return outputs;
    }

    private List<LootOutput> munchLootFunctions(JsonElement lootFunction, List<LootOutput> outputs) {
        JsonObject functionObject = lootFunction.getAsJsonObject();
        String kind = new Identifier(functionObject.get("function").getAsString()).toString();

        boolean createNew = false;
        List<LootOutput> newOutputs = null;
        if (functionObject.has("conditions")) {
            newOutputs = new ArrayList<>();
            for (LootOutput output : outputs) {
                newOutputs.add(output.copy());
            }
        }
        if (kind.equals("minecraft:set_count") && functionObject.has("count") && functionObject.get("count").isJsonPrimitive()) {
            int count = functionObject.get("count").getAsInt();
            for (LootOutput output : outputs) {
                for (EntryStack stack : output.output) {
                    stack.setAmount(count);
                }
                output.setCountText(String.valueOf(count));
            }
            createNew = true;
        } else if (kind.equals("minecraft:set_count") && functionObject.has("count") && functionObject.get("count").isJsonObject()) {
            String type = new Identifier(functionObject.get("count").getAsJsonObject().get("type").getAsString()).toString();
            if (type.equalsIgnoreCase("minecraft:uniform")) {
                int min = MathHelper.floor(functionObject.get("count").getAsJsonObject().get("min").getAsFloat());
                int max = MathHelper.floor(functionObject.get("count").getAsJsonObject().get("max").getAsFloat());
                int no = max - min + 1;
                for (LootOutput output : outputs) {
                    EntryStack first = output.output.get(0);
                    while (output.output.size() < no) {
                        output.output.add(first.copy());
                    }
                    for (int i = 0; i < no; i++) {
                        output.output.get(i).setAmount(min + i);
                    }
                    output.setCountText(I18n.translate("rer.loot.range", min, max));
                }
                createNew = true;
            }
        } else if (kind.equals("minecraft:limit_count") && functionObject.has("limit") && functionObject.get("limit").isJsonObject()) {
            Integer min = functionObject.get("limit").getAsJsonObject().has("min") ? functionObject.get("limit").getAsJsonObject().get("min").getAsInt() : null;
            Integer max = functionObject.get("limit").getAsJsonObject().has("max") ? functionObject.get("limit").getAsJsonObject().get("max").getAsInt() : null;
            for (LootOutput output : outputs) {
                for (EntryStack stack : output.output) {
                    if (min != null) {
                        int amount = stack.isEmpty() ? 0 : stack.getAmount();
                        if (amount < min) {
                            stack.setAmount(min);
                        }
                    }
                    if (max != null && stack.getAmount() > max) {
                        stack.setAmount(max);
                    }
                }
                if (output.output.isEmpty()) {
                    output.output.add(EntryStack.empty());
                }
                if (min != null)
                    output.addExtraTextCount(I18n.translate("rer.function.atLeast", min));
                if (max != null)
                    output.addExtraTextCount(I18n.translate("rer.function.atMost", max));
            }
            if (min != null || max != null)
                createNew = true;
        } else if (kind.equals("minecraft:copy_nbt") || kind.equals("minecraft:copy_name") || kind.equals("minecraft:explosion_decay") || kind.equals("minecraft:set_contents") || kind.equals("minecraft:copy_state")) {
            // Ignored
        } else if (kind.equals("minecraft:set_nbt") && functionObject.has("tag")) {
            try {
                CompoundTag tag = StringNbtReader.parse(JsonHelper.getString(functionObject, "tag"));
                for (LootOutput output : outputs) {
                    for (int i = 0; i < output.output.size(); i++) {
                        EntryStack stack = output.output.get(i).copy();
                        if (!stack.isEmpty() && stack.getType() == EntryStack.Type.ITEM) {
                            stack.getItemStack().getOrCreateTag().copyFrom(tag);
                        }
                        output.output.set(i, stack);
                    }
                    EntryStack stack = output.original.copy();
                    if (!stack.isEmpty() && stack.getType() == EntryStack.Type.ITEM) {
                        stack.getItemStack().getOrCreateTag().copyFrom(tag);
                    }
                    output.original = stack;
                }
                createNew = true;
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        } else if (kind.equals("minecraft:apply_bonus") && functionObject.has("enchantment")) {
            String enchantmentString = functionObject.get("enchantment").getAsString();
            Enchantment enchantment = Registry.ENCHANTMENT.get(new Identifier(enchantmentString));
            if (enchantment != null) {
                for (LootOutput output : outputs) {
                    output.addExtraTextCount(I18n.translate("rer.function.bonus.enchant", I18n.translate(enchantment.getTranslationKey()).toLowerCase()));
                }
                createNew = true;
            }
        } else if (kind.equals("minecraft:looting_enchant")) {
            for (LootOutput output : outputs) {
                output.addExtraTextCount(I18n.translate("rer.function.bonus.enchant", I18n.translate(Enchantments.LOOTING.getTranslationKey()).toLowerCase()));
            }
            createNew = true;
        } else if (kind.equals("minecraft:furnace_smelt")) {
            for (LootOutput output : outputs) {
                output.original = smelt(output.original);
                for (int i = 0; i < output.output.size(); i++) {
                    output.output.set(i, smelt(output.output.get(i)));
                }
            }
            createNew = true;
        } else {
            RERUtils.LOGGER.debug("Don't know how to deal with function of type %s (%s)", kind, functionObject);
        }

        if (functionObject.has("conditions")) {
            for (JsonElement conditionElement : functionObject.get("conditions").getAsJsonArray()) {
                munchLootCondition(conditionElement, outputs);
            }
        }

        if (functionObject.has("functions")) {
            List<LootOutput> newNewOutputs = new ArrayList<>();
            for (JsonElement functionElement : functionObject.get("functions").getAsJsonArray()) {
                List<LootOutput> list = munchLootFunctions(functionElement, outputs);
                if (list != null)
                    newNewOutputs.addAll(list);
            }
            outputs.addAll(newNewOutputs);
        }

        return createNew ? newOutputs : null;
    }

    @SuppressWarnings({"resource"}) // MinecraftClient.getInstance() is a singleton, and won't actually leak.
    private EntryStack smelt(EntryStack stack) {
        if (stack.isEmpty() || stack.getType() != EntryStack.Type.ITEM)
            return stack.copy();
        ClientWorld world = MinecraftClient.getInstance().world;
        Optional<SmeltingRecipe> optional = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new BasicInventory(stack.getItemStack()), world);
        if (optional.isPresent()) {
            ItemStack itemStack = optional.get().getOutput();
            if (!itemStack.isEmpty()) {
                EntryStack entryStack = EntryStack.create(itemStack.copy());
                entryStack.setAmount(stack.getAmount());
                return entryStack;
            }
        }
        return stack.copy();
    }

    public List<LootOutput> munchLootPoolJson(JsonObject poolObject) {
        List<LootOutput> outputs = new ArrayList<>();

        JsonArray entries = poolObject.getAsJsonObject().get("entries").getAsJsonArray();
        for (JsonElement entryElement : entries) {
            JsonObject entryObject = entryElement.getAsJsonObject();
            outputs.addAll(munchLootEntryJson(entryObject));
        }

        if (poolObject.getAsJsonObject().has("conditions")) {
            for (JsonElement conditionElement : poolObject.getAsJsonObject().get("conditions").getAsJsonArray()) {
                munchLootCondition(conditionElement, outputs);
            }
        }

        if (poolObject.getAsJsonObject().has("functions")) {
            List<LootOutput> newOutputs = new ArrayList<>();
            for (JsonElement functionElement : poolObject.getAsJsonObject().get("functions").getAsJsonArray()) {
                List<LootOutput> list = munchLootFunctions(functionElement, outputs);
                if (list != null)
                    newOutputs.addAll(list);
            }
            outputs.addAll(newOutputs);
        }

        return outputs;
    }

    public List<LootOutput> munchLootSupplierJson(JsonElement jsonSupplier) {
        List<LootOutput> outputs = new ArrayList<>();

        if (!jsonSupplier.getAsJsonObject().has("pools")) {
            return outputs;
        }

        JsonArray pools = jsonSupplier.getAsJsonObject().get("pools").getAsJsonArray();
        for (JsonElement poolElement : pools) {
            JsonObject poolObject = poolElement.getAsJsonObject();

            outputs.addAll(munchLootPoolJson(poolObject));
        }

        return outputs;
    }

    public List<LootOutput> getOutputs() {
        String json = ClientLootCache.ID_TO_LOOT.get(lootTableId);
        if (json == null)
            return Collections.emptyList();
        if (outputs == null || FabricLoader.getInstance().isDevelopmentEnvironment()) {
            outputs = munchLootSupplierJson(RoughlyEnoughResources.GSON.fromJson(json, JsonElement.class));
        }
        return outputs;
    }

    //    abstract boolean fillContextBuilder(Builder contextBuilder, World world);
}
