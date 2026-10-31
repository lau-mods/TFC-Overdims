package net.claustra01.tfcoverdims.worldgen;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class HardenUndersideFeature extends Feature<NoneFeatureConfiguration> {
    private final Block raw;
    private final Supplier<? extends Block> hardened;

    public HardenUndersideFeature(Codec<NoneFeatureConfiguration> codec, Block raw, Supplier<? extends Block> hardened) {
        super(codec);
        this.raw = raw;
        this.hardened = hardened;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        var structureManager = level.getLevel().structureManager();
        if (level instanceof WorldGenRegion region) {
            structureManager = structureManager.forWorldGenRegion(region);
        }
        var structures = structureManager.startsForStructure(chunk, structure -> true);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = level.getMinBuildHeight() + 1; y < level.getMaxBuildHeight(); y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).is(raw)) {
                        continue;
                    }
                    var below = level.getBlockState(pos.below());
                    if ((below.is(Blocks.AIR) || below.is(Blocks.CAVE_AIR))
                        && structures.stream().noneMatch(start -> start.getBoundingBox().isInside(pos))) {
                        level.setBlock(pos, hardened.get().defaultBlockState(), 2);
                    }
                }
            }
        }
        return true;
    }
}
