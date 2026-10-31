package net.claustra01.tfcoverdims.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.common.blocks.OverdimsBlocks;

public final class OverdimsFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TFCOverdims.MOD_ID);

    static {
        FEATURES.register("harden_nether_ceiling", () -> new HardenUndersideFeature(NoneFeatureConfiguration.CODEC, Blocks.NETHERRACK, OverdimsBlocks.HARDENED_NETHERRACK));
        FEATURES.register("harden_end_underside", () -> new HardenUndersideFeature(NoneFeatureConfiguration.CODEC, Blocks.END_STONE, OverdimsBlocks.HARDENED_END_STONE));
    }

    private OverdimsFeatures() {}
}
