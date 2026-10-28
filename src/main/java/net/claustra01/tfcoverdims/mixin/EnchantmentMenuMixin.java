package net.claustra01.tfcoverdims.mixin;

import net.claustra01.tfcoverdims.common.EnchantingLapis;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Redirect(method = "quickMoveStack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean acceptsLapis(ItemStack stack, Item item) {
        return EnchantingLapis.accepts(stack);
    }
}
