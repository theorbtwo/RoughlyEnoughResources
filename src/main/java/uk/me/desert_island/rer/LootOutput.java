package uk.me.desert_island.rer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

public class LootOutput {
    public String extra_text;
    public List<ItemStack> extra_inputs;
    public ItemStack output;

    public LootOutput() {
        this.extra_inputs = new ArrayList<ItemStack>();
    }

    public LootOutput copy() {
        LootOutput out = new LootOutput();
        out.extra_text = this.extra_text;
        out.extra_inputs.addAll(this.extra_inputs);
        out.output = this.output;

        return out;
    }

    public void add_extra_input(ItemStack extra) {
        extra_inputs.add(extra);
    }

    public void add_extra_text(String text) {
        if (extra_text != null) {
            extra_text = extra_text + ", " + text;
        } else {
            extra_text = text;
        }
    }

    @Override
    public String toString() {
        return "LootOutput [extra_inputs=" + extra_inputs + ", extra_text=" + extra_text + ", output=" + output + "]";
    }

}