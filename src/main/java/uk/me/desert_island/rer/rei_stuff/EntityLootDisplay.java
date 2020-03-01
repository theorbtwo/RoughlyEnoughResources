package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.rei.api.EntryStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.loot.context.LootContext.Builder;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class EntityLootDisplay extends LootDisplay {

    private final EntityType<?> inputEntity;

    public EntityLootDisplay(EntityType<?> inputEntity) {
        this.inputEntity = inputEntity;
        this.inputStack = EntryStack.create(SpawnEggItem.forEntity(inputEntity));
        this.dropTableId = inputEntity.getLootTableId();
        this.contextType = LootContextTypes.ENTITY;
    }

    @Override
    public Identifier getLocation() {
        return Registry.ENTITY_TYPE.getId(inputEntity);
    }

    @Override
    boolean fillContextBuilder(Builder contextBuilder, World world) {
        Entity entity = inputEntity.create(world);

        if (entity == null) {
            return false;
        }

        contextBuilder.put(LootContextParameters.POSITION, new net.minecraft.util.math.BlockPos(0, 0, 0));
        contextBuilder.put(LootContextParameters.THIS_ENTITY, entity);
        contextBuilder.put(LootContextParameters.DAMAGE_SOURCE, DamageSource.SWEET_BERRY_BUSH);

        return true;
    }
}
