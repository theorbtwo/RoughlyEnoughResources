package uk.me.desert_island.rer;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.api.REIHelper;
import me.shedaniel.rei.api.widgets.Tooltip;
import me.shedaniel.rei.impl.RenderingEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import uk.me.desert_island.rer.mixin.FluidBlockHooks;

public class RERUtils {

    public static final Logger LOGGER;

    static {
        LOGGER = new Logger();
    }

    public static class Logger {
        org.apache.logging.log4j.Logger logger = LogManager.getFormatterLogger("rer-ru");

        public void info(String str) {
            logger.info("[RER] " + str);
        }

        public void info(String str, Object... args) {
            logger.info("[RER] " + str, args);
        }

        public void error(String str) {
            logger.error("[RER] " + str);
        }

        public void error(String str, Object... args) {
            logger.error("[RER] " + str, args);
        }

        public void fatal(String str) {
            logger.fatal("[RER] " + str);
        }

        public void fatal(String str, Object... args) {
            logger.fatal("[RER] " + str, args);
        }

        public void warn(String str) {
            logger.warn("[RER] " + str);
        }

        public void warn(String str, Object... args) {
            logger.warn("[RER] " + str, args);
        }

        public void debug(String str) {
            if (FabricLoader.getInstance().isDevelopmentEnvironment())
                logger.debug("[RER] " + str);
        }

        public void debug(String str, Object... args) {
            if (FabricLoader.getInstance().isDevelopmentEnvironment())
                logger.debug("[RER] " + str, args);
        }
    }

    public static ItemStack fromWorldToItemStack(RegistryKey<World> dt) {
        if (dt == World.OVERWORLD) {
            return new ItemStack(Items.GRASS_BLOCK);
        }
        if (dt == World.NETHER) {
            return new ItemStack(Items.NETHERRACK);
        }
        if (dt == World.END) {
            return new ItemStack(Items.END_STONE);
        }
        return new ItemStack(Items.GLASS);
    }

    @Environment(EnvType.CLIENT)
    public static EntryStack fromBlockToItemStackWithText(Block block) {
        EntryStack stack = fromBlockToItemStack(block);
        if (stack.isEmpty()) {
            return new RenderingEntry() {
                @Override
                public void render(MatrixStack matrices, Rectangle rectangle, int mouseX, int mouseY, float delta) {
                    MinecraftClient instance = MinecraftClient.getInstance();
                    TextRenderer font = instance.textRenderer;
                    String text = "?";
                    int width = font.getStringWidth(text);
                    font.draw(matrices, text, rectangle.getCenterX() - width / 2f + 0.2f, rectangle.getCenterY() - font.fontHeight / 2f + 1f, REIHelper.getInstance().isDarkThemeEnabled() ? -4473925 : -12566464);
                }

                @Override
                public @Nullable Tooltip getTooltip(Point mouse) {
                    return Tooltip.create(mouse, block.getName());
                }
            };
        }
        return stack;
    }

    @Environment(EnvType.CLIENT)
    public static EntryStack fromBlockToItemStack(Block block) {
        Item item = block.asItem();

        if (block instanceof FluidBlock) {
            return EntryStack.create(((FluidBlockHooks) block).getFluid());
        }

        if (block == Blocks.FIRE) {
            return EntryStack.create(Items.FLINT_AND_STEEL);
        }

        ItemStack picked;
        try {
            picked = block.getPickStack(null, null, block.getDefaultState());
        } catch (Exception e) {
            picked = null;
        }

        if (picked != null && picked != ItemStack.EMPTY) {
            return EntryStack.create(picked);
        }

        return EntryStack.create(item);
    }
}