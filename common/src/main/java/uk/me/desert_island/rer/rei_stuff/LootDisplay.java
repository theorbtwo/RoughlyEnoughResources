package uk.me.desert_island.rer.rei_stuff;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.platform.Platform;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import uk.me.desert_island.rer.RERUtils;
import uk.me.desert_island.rer.client.ClientLootCache;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@SuppressWarnings("StatementWithEmptyBody")
@Environment(EnvType.CLIENT)
public abstract class LootDisplay implements Display {
    public EntryStack<?> inputStack;
    public ResourceLocation lootTableId;
    public LootContextParamSet contextType;
    public List<LootOutput> outputs = null;
    public static final NumberFormat FORMAT = new DecimalFormat("#.##");
    public static final NumberFormat FORMAT_MORE = new DecimalFormat("#.####");

    @Override
    public List<EntryIngredient> getInputEntries() {
        return Collections.singletonList(EntryIngredient.of(this.inputStack));
    }

    public abstract ResourceLocation getLocation();

    @Override
    public List<EntryIngredient> getOutputEntries() {
        EntryIngredient.Builder stacks = EntryIngredient.builder();
        for (LootOutput output : getOutputs()) {
            stacks.add(output.original);
        }
        return Collections.singletonList(stacks.build());
    }

    /*public List<EntryStack> getFlattenedOutputEntries() {
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
    }*/

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
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
        Item item = Registry.ITEM.get(new ResourceLocation(object.get("name").getAsString()));
        EntryStack<?> stack = EntryStacks.of(item);
        LootOutput output = new LootOutput();
        output.output = EntryIngredient.of(stack);
        output.original = stack.copy();
        List<LootOutput> outputs = new ArrayList<>();
        outputs.add(output);
        return outputs;
    }

    public void munchLootCondition(JsonElement conditionElement, List<LootOutput> outputs) {
        JsonObject conditionObject = conditionElement.getAsJsonObject();
        String kind = new ResourceLocation(conditionObject.get("condition").getAsString()).toString();

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
                output.addExtraText(I18n.get("rer.condition.killedByPlayer"));
            }
        } else if (kind.equals("minecraft:survives_explosion")) {
            /* Do nothing, this is generally just confusing. */
        } else if (kind.equals("minecraft:entity_properties") && conditionObject.has("predicate") && conditionObject.get("predicate").getAsJsonObject().has("flags") && conditionObject.get("predicate").getAsJsonObject().get("flags").getAsJsonObject().has("is_on_fire") && conditionObject.get("predicate").getAsJsonObject().get("flags").getAsJsonObject().get("is_on_fire").getAsBoolean() && conditionObject.has("entity") && conditionObject.get("entity").getAsString().equals("this")) {
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.get("rer.condition.onFire"));
            }
        } else if (kind.equals("minecraft:block_state_property") || kind.equals("minecraft:entity_properties") || kind.equals("minecraft:damage_source_properties")) {
            // ignore
        } else if (kind.equals("minecraft:match_tool") && conditionObject.has("predicate") && conditionObject.get("predicate").getAsJsonObject().has("enchantments")) {
            for (JsonElement enchantmentElement : conditionObject.get("predicate").getAsJsonObject().get("enchantments").getAsJsonArray()) {
                JsonObject enchantmentObject = enchantmentElement.getAsJsonObject();
                String enchantmentString = enchantmentObject.get("enchantment").getAsString();
                Enchantment enchantment = Registry.ENCHANTMENT.get(new ResourceLocation(enchantmentString));
                if (enchantment != null) {
                    for (LootOutput output : outputs) {
                        output.addExtraText(I18n.get("rer.condition.enchantment", I18n.get(enchantment.getDescriptionId()).toLowerCase()));
                    }
                }
            }
        } else if (kind.equals("minecraft:random_chance") && conditionObject.has("chance")) {
            double chance = conditionObject.get("chance").getAsDouble() * 100.0;
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.get("rer.condition.chance", FORMAT.format(chance)));
            }
        } else if (kind.equals("minecraft:random_chance_with_looting") && conditionObject.has("chance")) {
            double chance = conditionObject.get("chance").getAsDouble() * 100.0;
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.get("rer.condition.chance.looting", FORMAT.format(chance)));
            }
        } else if (kind.equals("minecraft:match_tool") && conditionObject.has("predicate") && conditionObject.get("predicate").getAsJsonObject().has("item")) {
            String itemId = conditionObject.get("predicate").getAsJsonObject().get("item").getAsString();
            Item item = Registry.ITEM.get(new ResourceLocation(itemId));
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.get("rer.condition.item", item.getDescription().getString().toLowerCase()));
            }
        } else if (kind.equals("minecraft:table_bonus")) {
            double chance = conditionObject.get("chances").getAsJsonArray().get(0).getAsDouble() * 100.0;
            for (LootOutput output : outputs) {
                output.addExtraText(I18n.get("rer.condition.chance", FORMAT.format(chance)));
            }
        } else {
            RERUtils.LOGGER.debug("Don't know how to deal with condition of type %s (%s)", kind, conditionObject);
        }
    }

    public List<LootOutput> munchLootEntryJson(JsonObject object) {
        String type = new ResourceLocation(object.get("type").getAsString()).toString();

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
                JsonElement json = ClientLootCache.ID_TO_LOOT.get(new ResourceLocation(object.get("name").getAsString()));
                if (json != null)
                    outputs.addAll(munchLootSupplierJson(json));
                break;
            case "minecraft:tag":
                TagKey<Item> tag = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(object.get("name").getAsString()));
                if (tag != null)
                    outputs.addAll(StreamSupport.stream(Registry.ITEM.getTagOrEmpty(tag).spliterator(), false).map(Holder::value).map(item -> {
                        EntryStack<?> stack = EntryStacks.of(item);
                        LootOutput output = new LootOutput();
                        output.output = EntryIngredient.of(stack);
                        output.original = stack.copy();
                        return output;
                    }).toList());
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

    public static Integer tryGetNumber(JsonObject obj, String field) {
        var val = obj.get(field);
        if (val == null || !val.isJsonPrimitive()) {
            return null;
        }

        return val.getAsInt();
    }

    private List<LootOutput> munchLootFunctions(JsonElement lootFunction, List<LootOutput> outputs) {
        JsonObject functionObject = lootFunction.getAsJsonObject();
        String kind = new ResourceLocation(functionObject.get("function").getAsString()).toString();

        boolean createNew = false;
        List<LootOutput> newOutputs = null;
        if (functionObject.has("conditions")) {
            newOutputs = new ArrayList<>();
            for (LootOutput output : outputs) {
                newOutputs.add(output.copy());
            }
        }
        if (kind.equals("minecraft:set_count") && functionObject.has("count")) {
            JsonElement countEl = functionObject.get("count");
            if (countEl.isJsonPrimitive()) {
                createNew = true;
                int count = countEl.getAsInt();
                for (LootOutput output : outputs) {
                    for (EntryStack<?> stack : output.output) {
                        stack.<ItemStack>castValue().setCount(count);
                    }
                    output.setCountText(String.valueOf(count));
                }
            } else if (countEl.isJsonObject()) {
                String type = new ResourceLocation(countEl.getAsJsonObject().get("type").getAsString()).toString();
                if (type.equalsIgnoreCase("minecraft:uniform")) {
                    Integer min = tryGetNumber(countEl.getAsJsonObject(), "min");
                    Integer max = tryGetNumber(countEl.getAsJsonObject(), "max");
                    int no = max != null && min != null ? max - min + 1 : 0;
                    for (LootOutput output : outputs) {
                        ArrayList<EntryStack<?>> newList = new ArrayList<>(output.output);
                        EntryStack<?> first = output.output.get(0);
                        while (newList.size() < no) {
                            newList.add(first.copy());
                        }
                        for (int i = 0; i < no; i++) {
                            ItemStack value = newList.get(i).castValue();
                            value.setCount(min + i);
                            value.getOrCreateTag().putInt("RER_COUNT", min + i);
                        }
                        output.output = EntryIngredient.of(newList);
                        if (min != null && max != null)
                            output.setCountText(I18n.get("rer.loot.range", min, max));
                        else if (min != null)
                            output.setCountText(I18n.get("rer.function.atLeast", min));
                        else if (max != null)
                            output.setCountText(I18n.get("rer.function.atMost", max));
                    }
                    createNew = min != null || max != null;
                }
            }
        } else if (kind.equals("minecraft:limit_count") && functionObject.has("limit") && functionObject.get("limit").isJsonObject()) {
            Integer min = tryGetNumber(functionObject.get("limit").getAsJsonObject(), "min");
            Integer max = tryGetNumber(functionObject.get("limit").getAsJsonObject(), "max");
            for (LootOutput output : outputs) {
                for (EntryStack<?> stack : output.output) {
                    ItemStack value = stack.castValue();
                    if (min != null) {
                        int amount = stack.isEmpty() ? 0 : value.getCount();
                        if (amount < min) {
                            value.setCount(min);
                        }
                    }
                    if (max != null && value.getCount() > max) {
                        value.setCount(max);
                    }
                }
                if (output.output.isEmpty()) {
                    output.output = EntryIngredient.of(EntryStack.empty());
                }
                if (min != null)
                    output.addExtraTextCount(I18n.get("rer.function.atLeast", min));
                if (max != null)
                    output.addExtraTextCount(I18n.get("rer.function.atMost", max));
            }
            createNew = min != null || max != null;
        } else if (kind.equals("minecraft:copy_nbt") || kind.equals("minecraft:copy_name") || kind.equals("minecraft:explosion_decay") || kind.equals("minecraft:set_contents") || kind.equals("minecraft:copy_state")) {
            // Ignored
        } else if (kind.equals("minecraft:set_nbt") && functionObject.has("tag")) {
            try {
                CompoundTag tag = TagParser.parseTag(GsonHelper.getAsString(functionObject, "tag"));
                for (LootOutput output : outputs) {
                    ArrayList<EntryStack<?>> newList = new ArrayList<>(output.output);
                    for (int i = 0; i < newList.size(); i++) {
                        EntryStack<?> stack = newList.get(i).copy();
                        if (!stack.isEmpty() && stack.getType() == VanillaEntryTypes.ITEM) {
                            stack.<ItemStack>castValue().getOrCreateTag().merge(tag);
                        }
                        newList.set(i, stack);
                    }
                    output.output = EntryIngredient.of(newList);
                    EntryStack<?> stack = output.original.copy();
                    if (!stack.isEmpty() && stack.getType() == VanillaEntryTypes.ITEM) {
                        stack.<ItemStack>castValue().getOrCreateTag().merge(tag);
                    }
                    output.original = stack;
                }
                createNew = true;
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        } else if (kind.equals("minecraft:apply_bonus") && functionObject.has("enchantment")) {
            String enchantmentString = functionObject.get("enchantment").getAsString();
            Enchantment enchantment = Registry.ENCHANTMENT.get(new ResourceLocation(enchantmentString));
            if (enchantment != null) {
                for (LootOutput output : outputs) {
                    output.addExtraTextCount(I18n.get("rer.function.bonus.enchant", I18n.get(enchantment.getDescriptionId()).toLowerCase()));
                }
                createNew = true;
            }
        } else if (kind.equals("minecraft:looting_enchant")) {
            for (LootOutput output : outputs) {
                output.addExtraTextCount(I18n.get("rer.function.bonus.enchant", I18n.get(Enchantments.MOB_LOOTING.getDescriptionId()).toLowerCase()));
            }
            createNew = true;
        } else if (kind.equals("minecraft:furnace_smelt")) {
            for (LootOutput output : outputs) {
                ArrayList<EntryStack<?>> newList = new ArrayList<>(output.output);
                output.original = smelt(output.original);
                for (int i = 0; i < newList.size(); i++) {
                    newList.set(i, smelt(newList.get(i)));
                }
                output.output = EntryIngredient.of(newList);
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
    private EntryStack<?> smelt(EntryStack<?> stack) {
        if (stack.isEmpty() || stack.getType() != VanillaEntryTypes.ITEM)
            return stack.copy();
        ClientLevel world = Minecraft.getInstance().level;
        Optional<SmeltingRecipe> optional = Minecraft.getInstance().getConnection().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack.<ItemStack>castValue()), world);
        if (optional.isPresent()) {
            ItemStack itemStack = optional.get().getResultItem();
            if (!itemStack.isEmpty()) {
                EntryStack<?> entryStack = EntryStacks.of(itemStack.copy());
                entryStack.<ItemStack>castValue().setCount(stack.<ItemStack>castValue().getCount());
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
        JsonElement json = ClientLootCache.ID_TO_LOOT.get(lootTableId);
        if (json == null)
            return Collections.emptyList();
        if (outputs == null || Platform.isDevelopmentEnvironment()) {
            try {
                outputs = munchLootSupplierJson(json);
            } catch (Exception e) {
                RERUtils.LOGGER.warn("Failed to parse loot table '%s': ", lootTableId, e);
                outputs = Collections.emptyList();
            }
        }
        return outputs;
    }

    //    abstract boolean fillContextBuilder(Builder contextBuilder, World world);
}
