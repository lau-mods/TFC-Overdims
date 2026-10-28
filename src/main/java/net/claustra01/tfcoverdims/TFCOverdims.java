package net.claustra01.tfcoverdims;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import net.claustra01.tfcoverdims.climate.OverdimsClimateModels;
import net.claustra01.tfcoverdims.common.blocks.OverdimsBlocks;
import net.claustra01.tfcoverdims.common.items.OverdimsItems;
import net.claustra01.tfcoverdims.common.wood.OverdimsWoodBlocks;
import net.claustra01.tfcoverdims.common.wood.OverdimsWood;
import net.claustra01.tfcoverdims.events.NetherFungusEvents;

@Mod(TFCOverdims.MOD_ID)
public final class TFCOverdims {
    public static final String MOD_ID = "tfcoverdims";

    public TFCOverdims(IEventBus modBus) {
        OverdimsWood.registerWoodTypes();
        OverdimsWoodBlocks.register(modBus);
        OverdimsBlocks.BLOCKS.register(modBus);
        OverdimsItems.ITEMS.register(modBus);
        OverdimsClimateModels.TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(OverdimsClimateModels::selectClimateModel);
        NeoForge.EVENT_BUS.addListener(NetherFungusEvents::bonemeal);
    }
}
