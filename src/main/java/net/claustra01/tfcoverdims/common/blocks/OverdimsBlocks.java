package net.claustra01.tfcoverdims.common.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.dries007.tfc.common.blocks.rock.Rock;

public final class OverdimsBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TFCOverdims.MOD_ID);
    public static final DeferredBlock<LooseRockBlock> LOOSE_NETHERRACK = register("loose_netherrack");
    public static final DeferredBlock<LooseRockBlock> LOOSE_BLACKSTONE = register("loose_blackstone");
    public static final DeferredBlock<LooseRockBlock> LOOSE_END_STONE = register("loose_end_stone");
    public static final DeferredBlock<Block> COBBLE_NETHERRACK = registerRock("cobble_netherrack", Rock.BlockType.COBBLE, Rock.BASALT);
    public static final DeferredBlock<Block> HARDENED_NETHERRACK = registerRock("hardened_netherrack", Rock.BlockType.HARDENED, Rock.BASALT);
    public static final DeferredBlock<Block> COBBLE_END_STONE = registerRock("cobble_end_stone", Rock.BlockType.COBBLE, Rock.LIMESTONE);
    public static final DeferredBlock<Block> HARDENED_END_STONE = registerRock("hardened_end_stone", Rock.BlockType.HARDENED, Rock.LIMESTONE);

    private static DeferredBlock<Block> registerRock(String name, Rock.BlockType type, Rock rock) {
        return BLOCKS.register(name, () -> type.create(rock));
    }

    private static DeferredBlock<LooseRockBlock> register(String name) {
        return BLOCKS.register(name, () -> new LooseRockBlock(BlockBehaviour.Properties.of()
            .strength(0.05f, 0f).sound(SoundType.STONE).noCollission()));
    }
}
