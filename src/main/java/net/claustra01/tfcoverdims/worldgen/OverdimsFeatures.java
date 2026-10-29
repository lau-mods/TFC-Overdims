package net.claustra01.tfcoverdims.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.claustra01.tfcoverdims.TFCOverdims;

public final class OverdimsFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TFCOverdims.MOD_ID);

    static {
        FEATURES.register("harden_nether_ceiling", () -> new HardenNetherCeilingFeature(NoneFeatureConfiguration.CODEC));
    }

    private OverdimsFeatures() {}
}
