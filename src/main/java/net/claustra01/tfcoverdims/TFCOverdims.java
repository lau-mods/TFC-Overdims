package net.claustra01.tfcoverdims;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import net.claustra01.tfcoverdims.climate.OverdimsClimateModels;
import net.claustra01.tfcoverdims.common.blocks.OverdimsBlocks;
import net.claustra01.tfcoverdims.common.items.OverdimsItems;

@Mod(TFCOverdims.MOD_ID)
public final class TFCOverdims {
    public static final String MOD_ID = "tfcoverdims";

    public TFCOverdims(IEventBus modBus) {
        OverdimsBlocks.BLOCKS.register(modBus);
        OverdimsItems.ITEMS.register(modBus);
        OverdimsClimateModels.TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(OverdimsClimateModels::selectClimateModel);
    }
}
