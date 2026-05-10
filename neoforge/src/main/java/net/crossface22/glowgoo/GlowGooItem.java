package net.crossface22.glowgoo;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class GlowGooItem extends SnowballItem {
    public GlowGooItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var state = level.getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof GlowGooBlock)) {
            return super.useOn(context);
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        int current = state.getValue(GlowGooBlock.LIGHT_LEVEL);
        int next = player.isShiftKeyDown()
                ? (current == 1 ? 15 : current - 1)
                : (current == 15 ? 1 : current + 1);

        if (!level.isClientSide) {
            level.setBlock(context.getClickedPos(), state.setValue(GlowGooBlock.LIGHT_LEVEL, next), Block.UPDATE_ALL);
            float pitch = 0.5F + (next - 1) * (1.5F / 14F);
            level.playSound(null, context.getClickedPos(), SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.3F, pitch);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        GlowGooProjectile projectile = new GlowGooProjectile(level, position.x(), position.y(), position.z());
        projectile.setItem(stack.copyWithCount(1));
        return projectile;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hitResult = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (hitResult instanceof BlockHitResult blockHitResult) {
            var state = level.getBlockState(blockHitResult.getBlockPos());
            if (state.is(GlowGoo.glowGooBlock())) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );

        if (!level.isClientSide) {
            GlowGooProjectile projectile = new GlowGooProjectile(level, player);
            projectile.setItem(stack);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(projectile);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void registerDispenserBehavior(Item item) {
        DispenserBlock.registerBehavior(item, new ProjectileDispenseBehavior(item));
    }
}
