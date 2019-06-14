package uk.me.desert_island.rer;

import me.shedaniel.rei.api.REIPluginEntry;
import net.minecraft.util.Identifier;

public class REIPlugins implements REIPluginEntry {
    public static final Identifier PLUGIN_ID = new Identifier("roughlyenoughresources", "rer_plugin");
    
    @Override public Identifier getPluginIdentifier() {
	return PLUGIN_ID;
    }
}
