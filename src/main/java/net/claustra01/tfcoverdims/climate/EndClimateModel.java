package net.claustra01.tfcoverdims.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.climate.ClimateModelType;

public enum EndClimateModel implements ClimateModel {
    INSTANCE;

    @Override
    public ClimateModelType<?> type() {
        return OverdimsClimateModels.END.get();
    }

    @Override
    public float getAverageTemperature(LevelReader level, BlockPos pos) {
        return 20f;
    }

    @Override
    public float getAverageRainfall(LevelReader level, BlockPos pos) {
        return 0f;
    }
}
