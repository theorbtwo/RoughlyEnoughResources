package uk.me.desert_island.rer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.platform.Platform;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.AbstractRenderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.client.util.ClientEntryStacks;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class RERUtils {
    public static final L LOGGER;

    static {
        LOGGER = new L();
    }

    public static class L {
        private final Logger logger = LogManager.getFormatterLogger("rer-ru");

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
            if (Platform.isDevelopmentEnvironment()) {
                logger.debug("[RER] " + str);
            }
        }

        public void debug(String str, Object... args) {
            if (Platform.isDevelopmentEnvironment()) {
                logger.debug("[RER] " + str, args);
            }
        }
    }

    public static ItemStack fromWorldToItemStack(ResourceKey<Level> dt) {
        Item item;
        if (dt == Level.OVERWORLD) {
            item = Items.GRASS_BLOCK;
        } else if (dt == Level.NETHER) {
            item = Items.NETHERRACK;
        } else if (dt == Level.END) {
            item = Items.END_STONE;
        } else {
            item = Items.GLASS;
        }
        return new ItemStack(item);
    }

    @Environment(EnvType.CLIENT)
    public static EntryStack<?> fromBlockToItemStackWithText(Block block) {
        EntryStack<?> stack = fromBlockToItemStack(block);
        if (stack.isEmpty()) {
            return ClientEntryStacks.of(new AbstractRenderer() {
                @Override
                public void render(PoseStack matrices, Rectangle rectangle, int i, int i1, float v) {
                    Minecraft instance = Minecraft.getInstance();
                    Font font = instance.font;
                    String text = "?";
                    int width = font.width(text);
                    font.draw(matrices, text, rectangle.getCenterX() - width / 2f + 0.2f, rectangle.getCenterY() - font.lineHeight / 2f + 1f, REIRuntime.getInstance().isDarkThemeEnabled() ? -4473925 : -12566464);
                }

                @Override
                public @Nullable Tooltip getTooltip(TooltipContext mouse) {
                    return Tooltip.create(mouse.getPoint(), block.getName());
                }
            });
        }
        return stack;
    }

    @Environment(EnvType.CLIENT)
    public static EntryStack<?> fromBlockToItemStack(Block block) {
        Item item = block.asItem();

        if (block instanceof LiquidBlock liquidBlock) {
            return EntryStacks.of(liquidBlock.getFluidState(block.defaultBlockState()).getType());
        }

        if (block == Blocks.FIRE) {
            return EntryStacks.of(Items.FLINT_AND_STEEL);
        }

        ItemStack picked;
        try {
            picked = block.getCloneItemStack(null, null, block.defaultBlockState());
        } catch (Exception e) {
            picked = null;
        }

        if (picked != null && !picked.isEmpty()) {
            return EntryStacks.of(picked);
        }

        return EntryStacks.of(item);
    }
}