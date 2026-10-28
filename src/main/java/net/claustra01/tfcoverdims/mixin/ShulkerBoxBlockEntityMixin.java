package net.claustra01.tfcoverdims.mixin;

import net.claustra01.tfcoverdims.common.ContainerRules;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin implements Container {
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems() && ContainerRules.fitsTfcChest(stack);
    }

    @Inject(method = "canPlaceItemThroughFace", at = @At("RETURN"), cancellable = true)
    private void restrictPlacementThroughFace(int slot, ItemStack stack, Direction side, CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(result.getReturnValue() && ContainerRules.fitsTfcChest(stack));
    }
}
