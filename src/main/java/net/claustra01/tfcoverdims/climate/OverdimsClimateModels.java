package net.claustra01.tfcoverdims.climate;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.dries007.tfc.util.climate.ClimateModelType;
import net.dries007.tfc.util.climate.ClimateModels;
import net.dries007.tfc.util.events.SelectClimateModelEvent;

import net.claustra01.tfcoverdims.TFCOverdims;

public final class OverdimsClimateModels {
    public static final DeferredRegister<ClimateModelType<?>> TYPES =
        DeferredRegister.create(ClimateModels.KEY, TFCOverdims.MOD_ID);

    public static final DeferredHolder<ClimateModelType<?>, ClimateModelType<NetherClimateModel>> NETHER =
        TYPES.register("nether", () -> new ClimateModelType<>(StreamCodec.unit(NetherClimateModel.INSTANCE)));
    public static final DeferredHolder<ClimateModelType<?>, ClimateModelType<EndClimateModel>> END =
        TYPES.register("end", () -> new ClimateModelType<>(StreamCodec.unit(EndClimateModel.INSTANCE)));

    public static void selectClimateModel(SelectClimateModelEvent event) {
        if (event.level().dimension() == Level.NETHER) {
            event.setModel(NetherClimateModel.INSTANCE);
        } else if (event.level().dimension() == Level.END) {
            event.setModel(EndClimateModel.INSTANCE);
        }
    }
}
