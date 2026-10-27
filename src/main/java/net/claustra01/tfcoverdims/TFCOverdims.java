package net.claustra01.tfcoverdims;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import net.claustra01.tfcoverdims.climate.OverdimsClimateModels;

@Mod(TFCOverdims.MOD_ID)
public final class TFCOverdims {
    public static final String MOD_ID = "tfcoverdims";

    public TFCOverdims(IEventBus modBus) {
        OverdimsClimateModels.TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(OverdimsClimateModels::selectClimateModel);
    }
}
