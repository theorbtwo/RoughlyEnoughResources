package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class LootOutput {
    public String countText;
    public String extraTextCount;
    public String extraText;
    public List<ItemStack> extraInputs;
    public EntryIngredient output;
    public EntryStack<?> original;
    public boolean nowInverted = false;
    public boolean lastInverted = false;

    public LootOutput() {
        this.extraInputs = new ArrayList<>();
    }

    public LootOutput copy() {
        LootOutput out = new LootOutput();
        out.countText = this.countText;
        out.extraText = this.extraText;
        out.extraTextCount = this.extraTextCount;
        for (ItemStack extraInput : this.extraInputs) {
            out.extraInputs.add(extraInput.copy());
        }
        EntryIngredient.Builder builder = EntryIngredient.builder();
        for (EntryStack<?> output : this.output) {
            builder.add(output.copy());
        }
        out.output = builder.build();
        out.original = this.original;

        return out;
    }

    public void addExtraInput(ItemStack extra) {
        extraInputs.add(extra);
    }

    public void addExtraText(String text) {
        if (nowInverted)
            text = I18n.get("rer.condition.invert", text);
        if (extraText != null) {
            if (lastInverted && nowInverted)
                extraText = I18n.get("rer.condition.or", extraText, text);
            else
                extraText = I18n.get("rer.function.and", extraText, text);
        } else {
            extraText = text;
        }
        lastInverted = nowInverted;
    }

    public void addExtraTextCount(String text) {
        if (extraTextCount != null) {
            extraTextCount = I18n.get("rer.function.and", extraTextCount, text);
        } else {
            extraTextCount = text;
        }
    }

    public void setCountText(String countText) {
        this.countText = countText;
    }

    @Override
    public String toString() {
        return "LootOutput [extraInputs=" + extraInputs + ", extraText=" + extraText + ", output=" + output + "]";
    }
}