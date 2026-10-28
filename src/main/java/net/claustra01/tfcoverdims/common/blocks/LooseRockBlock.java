package net.claustra01.tfcoverdims.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.GroundcoverBlock;
import net.dries007.tfc.util.Helpers;

// TFC's LooseRockBlock fixes count at 1–3. Its groundcover base keeps fluid handling and pickup.
public final class LooseRockBlock extends GroundcoverBlock {
    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, 4);

    public LooseRockBlock(Properties properties) {
        super(ExtendedProperties.of(properties), Shapes.empty());
        registerDefaultState(defaultBlockState().setValue(COUNT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COUNT);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(asItem())) {
            int count = state.getValue(COUNT);
            if (hand == InteractionHand.MAIN_HAND && count < 4) {
                BlockState next = state.setValue(COUNT, count + 1);
                if (next.canSurvive(level, pos)) {
                    Helpers.playPlaceSound(player, level, pos, state);
                    level.setBlockAndUpdate(pos, next);
                    stack.consume(1, player);
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP,
            state.getValue(COUNT) == 1 ? SupportType.CENTER : SupportType.FULL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(COUNT) == 1 ? box(4, 0, 4, 11, 2, 12) : box(0, 0, 0, 15, 2, 16);
    }
}
