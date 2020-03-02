package uk.me.desert_island.rer.rei_stuff;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.clothconfig2.api.ScissorsHandler;
import me.shedaniel.math.api.Rectangle;
import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.gui.widget.EntryWidget;
import me.shedaniel.rei.gui.widget.SlotBaseWidget;
import me.shedaniel.rei.gui.widget.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Quaternion;

import java.util.List;

public class EntityLootCategory extends LootCategory {
    public static final Identifier CATEGORY_ID = new Identifier("roughlyenoughresources", "entity_loot_category");

    @Override
    public Identifier getIdentifier() {
        return CATEGORY_ID;
    }

    @Override
    public EntryStack getLogo() {
        return EntryStack.create(Items.SPAWNER);
    }

    @Override
    public String getCategoryName() {
        return I18n.translate("rer.entity.loot.category");
    }

    @Override
    protected Rectangle getOutputsArea(Rectangle root) {
        return new Rectangle(root.x + 56, root.y, root.width - 55, root.height - 12);
    }

    @Override
    protected void registerWidget(LootDisplay display, List<Widget> widgets, Rectangle bounds) {
        EntityLootDisplay entityLootDisplay = (EntityLootDisplay) display;
        widgets.add(new EntityRendererWidget(new Rectangle(bounds.getMinX(), bounds.getMinY(), 54, 54), entityLootDisplay.getInputEntity()));
        widgets.add(EntryWidget.create(bounds.getMinX() + 54 - 18, bounds.getMinY() + 1).entry(entityLootDisplay.inputStack).noBackground().noHighlight());
    }

    private static class EntityRendererWidget extends SlotBaseWidget {
        private final Entity entity;

        public EntityRendererWidget(Rectangle bounds, EntityType<?> inputEntity) {
            super(bounds);
            this.entity = inputEntity.create(MinecraftClient.getInstance().world);
        }

        @Override
        public void render(int mouseX, int mouseY, float delta) {
            super.render(mouseX, mouseY, delta);
            Rectangle bounds = getBounds();
            ScissorsHandler.INSTANCE.scissor(new Rectangle(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2));
            float f = (float) Math.atan((bounds.getCenterX() - mouseX) / 40.0F);
            float g = (float) Math.atan((bounds.getCenterY() - mouseY) / 40.0F);
            float size = 32;
            if (Math.max(entity.getWidth(), entity.getHeight()) > 1.0) {
                size /= Math.max(entity.getWidth(), entity.getHeight());
            }
            RenderSystem.pushMatrix();
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.loadIdentity();
            matrix4f.multiply(Matrix4f.scale(5F, -0.5F, 0.5F));
            RenderSystem.setupLevelDiffuseLighting(matrix4f);
            RenderSystem.translatef(bounds.getCenterX(), bounds.getCenterY() + 20, 1050.0F);
            RenderSystem.scalef(1.0F, 1.0F, -1.0F);
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.translate(0.0D, 0.0D, 1000.0D);
            matrixStack.scale(size, size, size);
            Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(180.0F);
            Quaternion quaternion2 = Vector3f.POSITIVE_X.getDegreesQuaternion(g * 20.0F);
            quaternion.hamiltonProduct(quaternion2);
            matrixStack.multiply(quaternion);
            float i = entity.yaw;
            float j = entity.pitch;
            float h = 0, k = 0, l = 0;
            entity.yaw = 180.0F + f * 40.0F;
            entity.pitch = -g * 20.0F;
            if (entity instanceof LivingEntity) {
                h = ((LivingEntity) entity).bodyYaw;
                k = ((LivingEntity) entity).prevHeadYaw;
                l = ((LivingEntity) entity).headYaw;
                ((LivingEntity) entity).bodyYaw = 180.0F + f * 20.0F;
                ((LivingEntity) entity).headYaw = entity.yaw;
                ((LivingEntity) entity).prevHeadYaw = entity.yaw;
            }
            EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderManager();
            quaternion2.conjugate();
            entityRenderDispatcher.setRotation(quaternion2);
            entityRenderDispatcher.setRenderShadows(false);
            VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            entityRenderDispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrixStack, immediate, 15728880);
            immediate.draw();
            entityRenderDispatcher.setRenderShadows(true);
            entity.yaw = i;
            entity.pitch = j;
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).bodyYaw = h;
                ((LivingEntity) entity).prevHeadYaw = k;
                ((LivingEntity) entity).headYaw = l;
            }
            Matrix4f matrix4f2 = new Matrix4f();
            matrix4f2.loadIdentity();
            matrix4f2.multiply(Matrix4f.scale(3F, -1F, 2F));
            RenderSystem.setupLevelDiffuseLighting(matrix4f2);
            RenderSystem.popMatrix();
            ScissorsHandler.INSTANCE.removeLastScissor();
        }
    }
}
