package net.claustra01.tfcoverdims.climate;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.dries007.tfc.util.Helpers;

public final class ClimateDisplayName {
    private ClimateDisplayName() {}

    public static MutableComponent forDimension(ResourceKey<Level> dimension, Enum<?> classification) {
        if (dimension == Level.NETHER) {
            return Component.translatable("tfcoverdims.climate.nether");
        }
        if (dimension == Level.END) {
            return Component.translatable("tfcoverdims.climate.end");
        }
        return Helpers.translateEnum(classification);
    }
}
