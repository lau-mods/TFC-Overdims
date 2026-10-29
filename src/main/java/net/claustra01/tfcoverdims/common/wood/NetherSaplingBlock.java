package net.claustra01.tfcoverdims.common.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.wood.TFCSaplingBlock;

public final class NetherSaplingBlock extends TFCSaplingBlock {
    public NetherSaplingBlock(OverdimsWood wood, ExtendedProperties properties) {
        super(wood.tree(), properties, wood.ticksToGrow(), false);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(TFCTags.Blocks.TREE_GROWS_ON) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.dimension() != Level.NETHER) {
            super.randomTick(state, level, pos, random);
            return;
        }
        // Nether has no sky light; retain TFC's chance, area and Calendar checks.
        if (random.nextInt(7) == 0 && level.isAreaLoaded(pos, 1)
            && level.getBlockEntity(pos) instanceof TickCounterBlockEntity counter
            && counter.getTicksSinceUpdate() > getTicksToGrow()) {
            advanceTree(level, pos, state.setValue(STAGE, 1), random);
        }
    }
}
