package net.claustra01.tfcoverdims.climate;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.climate.ClimateModelType;

public enum NetherClimateModel implements ClimateModel {
    INSTANCE;

    private static final Map<ResourceKey<Biome>, Float> BASE_TEMPERATURES = Map.of(
        Biomes.NETHER_WASTES, 70f,
        Biomes.BASALT_DELTAS, 80f,
        Biomes.CRIMSON_FOREST, 60f,
        Biomes.WARPED_FOREST, 60f,
        Biomes.SOUL_SAND_VALLEY, 55f
    );

    @Override
    public ClimateModelType<?> type() {
        return OverdimsClimateModels.NETHER.get();
    }

    @Override
    public float getAverageTemperature(LevelReader level, BlockPos pos) {
        float base = BASE_TEMPERATURES.getOrDefault(
            level.getBiome(pos).unwrapKey().orElseThrow(), 70f);
        return base - Mth.clampedMap(pos.getY(), 32f, 128f, -5f, 5f);
    }

    @Override
    public float getAverageRainfall(LevelReader level, BlockPos pos) {
        return 0f;
    }
}
