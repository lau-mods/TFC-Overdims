package net.claustra01.tfcoverdims.mixin;

import net.claustra01.tfcoverdims.common.EnchantingLapis;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.inventory.EnchantmentMenu$3")
public abstract class EnchantmentMenuLapisSlotMixin {
    @Redirect(method = "mayPlace", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean acceptsLapis(ItemStack stack, Item item) {
        return EnchantingLapis.accepts(stack);
    }
}
