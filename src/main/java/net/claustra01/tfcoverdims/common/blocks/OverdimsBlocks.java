package net.claustra01.tfcoverdims.common.blocks;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.claustra01.tfcoverdims.TFCOverdims;

public final class OverdimsBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TFCOverdims.MOD_ID);
    public static final DeferredBlock<LooseRockBlock> LOOSE_NETHERRACK = register("loose_netherrack");
    public static final DeferredBlock<LooseRockBlock> LOOSE_BLACKSTONE = register("loose_blackstone");
    public static final DeferredBlock<LooseRockBlock> LOOSE_END_STONE = register("loose_end_stone");

    private static DeferredBlock<LooseRockBlock> register(String name) {
        return BLOCKS.register(name, () -> new LooseRockBlock(BlockBehaviour.Properties.of()
            .strength(0.05f, 0f).sound(SoundType.STONE).noCollission()));
    }
}
