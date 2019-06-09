package uk.me.desert_island.rer;

public class REIPlugins implements REIPluginEntry {
    public static final Identifier PLUGIN = new Identifier("roughlyenoughresources", "rer_plugin");
    
    @Override public Identifier getPluginIdentifier() {
	return PLUGIN_ID;
    }
}
