package net.claustra01.tfcoverdims.mixin;

import net.claustra01.tfcoverdims.common.ContainerRules;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxSlot.class)
public abstract class ShulkerBoxSlotMixin {
    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    private void restrictPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(result.getReturnValue() && ContainerRules.fitsTfcChest(stack));
    }
}
