package net.crossface22.glowgoo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class GlowGooBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 1, 15);

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.DOWN, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D));
        SHAPES.put(Direction.UP, Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D));
        SHAPES.put(Direction.NORTH, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D));
        SHAPES.put(Direction.SOUTH, Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D));
        SHAPES.put(Direction.WEST, Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D));
        SHAPES.put(Direction.EAST, Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));
    }

    public GlowGooBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIGHT_LEVEL)));
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.DOWN)
                .setValue(WATERLOGGED, false)
                .setValue(LIGHT_LEVEL, 15));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, LIGHT_LEVEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos attachedPos = pos.relative(facing);
        BlockState attachedState = level.getBlockState(attachedPos);
        return Block.isFaceFull(attachedState.getOcclusionShape(level, attachedPos), facing.getOpposite());
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (!state.canSurvive(level, pos)) {
            return state.getValue(WATERLOGGED)
                    ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                    : Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && shouldDropOnReplacement(state, newState, movedByPiston)) {
            Block.dropResources(state, level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(GlowGoo.glowGooItem());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean waterlogged = level.getFluidState(pos).getType() == Fluids.WATER;

        for (Direction direction : context.getNearestLookingDirections()) {
            Direction attachedFace = direction.getOpposite();
            BlockState trialState = defaultBlockState()
                    .setValue(FACING, attachedFace)
                    .setValue(WATERLOGGED, waterlogged);
            if (trialState.canSurvive(level, pos)) {
                return trialState;
            }
        }

        return null;
    }

    public boolean tryPlace(Level level, BlockPos pos, Direction facing) {
        BlockState replacedState = level.getBlockState(pos);
        if (!replacedState.canBeReplaced()) {
            return false;
        }

        boolean waterlogged = level.getFluidState(pos).getType() == Fluids.WATER;
        BlockState preferred = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, waterlogged)
                .setValue(LIGHT_LEVEL, 15);

        if (preferred.canSurvive(level, pos)) {
            level.setBlock(pos, preferred, UPDATE_ALL);
            return true;
        }

        for (Direction fallback : Direction.values()) {
            BlockState candidate = preferred.setValue(FACING, fallback);
            if (candidate.canSurvive(level, pos)) {
                level.setBlock(pos, candidate, UPDATE_ALL);
                return true;
            }
        }

        return false;
    }

    private static boolean shouldDropOnReplacement(BlockState state, BlockState newState, boolean movedByPiston) {
        if (movedByPiston || state.is(newState.getBlock())) {
            return false;
        }

        if (newState.isAir()) {
            return false;
        }

        FluidState fluidState = newState.getFluidState();
        if (fluidState.getType() == Fluids.WATER) {
            return state.getValue(WATERLOGGED);
        }

        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(GlowGoo.glowGooItem())) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        int current = state.getValue(LIGHT_LEVEL);
        int next = player.isShiftKeyDown()
                ? (current == 1 ? 15 : current - 1)
                : (current == 15 ? 1 : current + 1);

        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(LIGHT_LEVEL, next), UPDATE_ALL);
            float pitch = 0.5F + (next - 1) * (1.5F / 14F);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.3F, pitch);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
