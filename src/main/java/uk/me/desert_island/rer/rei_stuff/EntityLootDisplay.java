package uk.me.desert_island.rer.rei_stuff;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.world.World;
import net.minecraft.world.loot.context.LootContext.Builder;
import net.minecraft.world.loot.context.LootContextParameters;
import net.minecraft.world.loot.context.LootContextTypes;

public class EntityLootDisplay extends LootDisplay {

    private EntityType<?> in_entity;

    public EntityLootDisplay(EntityType<?> entity) {
        this.in_entity = entity;
        this.in_stack = new ItemStack(SpawnEggItem.forEntity(entity));
        this.drop_table_id = entity.getLootTableId();
        this.context_type = LootContextTypes.ENTITY;
	}

	@Override
    boolean fill_context_builder(Builder context_builder, World world) {
        Entity entity = in_entity.create(world);

        if (entity == null) {
            return false;
        }

        context_builder.put(LootContextParameters.POSITION, new net.minecraft.util.math.BlockPos(0, 0, 0));
        context_builder.put(LootContextParameters.THIS_ENTITY, entity);
        context_builder.put(LootContextParameters.DAMAGE_SOURCE, DamageSource.SWEET_BERRY_BUSH);

        return true;
    }
}
