package net.claustra01.tfcoverdims.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.dries007.tfc.client.screen.ClimateScreen;
import net.dries007.tfc.util.Helpers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.claustra01.tfcoverdims.climate.ClimateDisplayName;

@Mixin(value = ClimateScreen.class, remap = false)
public abstract class ClimateScreenMixin {
    @Redirect(method = "renderLabels", at = @At(value = "INVOKE",
        target = "Lnet/dries007/tfc/util/Helpers;translateEnum(Ljava/lang/Enum;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent dimensionClimateName(Enum<?> classification) {
        var level = Minecraft.getInstance().level;
        return level == null ? Helpers.translateEnum(classification)
            : ClimateDisplayName.forDimension(level.dimension(), classification);
    }
}
