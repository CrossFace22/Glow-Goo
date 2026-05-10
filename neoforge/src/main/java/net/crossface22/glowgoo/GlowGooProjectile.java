package net.crossface22.glowgoo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class GlowGooProjectile extends ThrowableItemProjectile {
    public GlowGooProjectile(EntityType<? extends GlowGooProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GlowGooProjectile(Level level, LivingEntity thrower) {
        super(GlowGoo.glowGooEntityType(), thrower, level);
    }

    public GlowGooProjectile(Level level, double x, double y, double z) {
        super(GlowGoo.glowGooEntityType(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return GlowGoo.glowGooItem();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (level().isClientSide) {
            return;
        }

        if (result instanceof EntityHitResult entityHitResult) {
            if (entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 20));
                level().playSound(
                        null,
                        blockPosition(),
                        SoundEvents.SLIME_SQUISH,
                        SoundSource.PLAYERS,
                        0.6F,
                        1.1F
                );
                level().broadcastEntityEvent(this, (byte) 3);
                discard();
                return;
            }
        }

        boolean placed = false;
        if (result instanceof BlockHitResult blockHitResult) {
            BlockPos hitPos = blockHitResult.getBlockPos();
            BlockPos placePos = hitPos.relative(blockHitResult.getDirection());

            if (level().getBlockState(hitPos).is(GlowGoo.glowGooBlock()) || level().getBlockState(placePos).is(GlowGoo.glowGooBlock())) {
                ItemStack itemStack = getItem().copyWithCount(1);
                level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), itemStack));
                level().broadcastEntityEvent(this, (byte) 3);
                discard();
                return;
            }

            Direction facing = blockHitResult.getDirection().getOpposite();
            placed = ((GlowGooBlock) GlowGoo.glowGooBlock()).tryPlace(level(), placePos, facing);

            if (placed) {
                level().playSound(
                        null,
                        placePos,
                        SoundEvents.SLIME_BLOCK_PLACE,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.2F + level().random.nextFloat() * 0.2F
                );
            }
        }

        if (!placed) {
            ItemStack itemStack = getItem().copyWithCount(1);
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), itemStack));
        }

        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }
}
