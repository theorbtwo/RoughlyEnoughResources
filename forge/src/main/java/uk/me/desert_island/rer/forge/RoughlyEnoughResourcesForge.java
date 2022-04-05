package uk.me.desert_island.rer.forge;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraftforge.fml.common.Mod;
import uk.me.desert_island.rer.RoughlyEnoughResources;
import uk.me.desert_island.rer.RoughlyEnoughResourcesClient;

@Mod("roughlyenoughresources")
public class RoughlyEnoughResourcesForge {
    public RoughlyEnoughResourcesForge() {
        RoughlyEnoughResources.onInitialize();
        EnvExecutor.runInEnv(Env.CLIENT, () -> Client::run);
    }

    private static class Client {
        public static void run() {
            RoughlyEnoughResourcesClient.onInitializeClient();
        }
    }
}
