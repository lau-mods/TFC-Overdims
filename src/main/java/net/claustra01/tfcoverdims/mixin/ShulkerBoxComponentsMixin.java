package net.claustra01.tfcoverdims.mixin;

import net.claustra01.tfcoverdims.common.ContainerRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseContainerBlockEntity.class)
public abstract class ShulkerBoxComponentsMixin {
    @Inject(method = "applyImplicitComponents", at = @At("TAIL"))
    private void restrictComponentContents(CallbackInfo callback) {
        if ((Object) this instanceof ShulkerBoxBlockEntity box) {
            // Item components copy directly into the inventory without calling canPlaceItem.
            for (int slot = 0; slot < box.getContainerSize(); slot++) {
                var stack = box.getItem(slot);
                if (!stack.isEmpty() && (!stack.getItem().canFitInsideContainerItems() || !ContainerRules.fitsTfcChest(stack))) {
                    box.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
    }
}
