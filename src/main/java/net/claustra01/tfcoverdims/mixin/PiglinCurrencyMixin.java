package net.claustra01.tfcoverdims.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Piglin.class)
public abstract class PiglinCurrencyMixin {
    @Unique
    private static final TagKey<Item> tfcoverdims$currency = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("tfcoverdims", "piglin_currency"));

    @Redirect(method = "holdInOffHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private boolean acceptCurrency(ItemStack stack) {
        return stack.isPiglinCurrency() || stack.is(tfcoverdims$currency);
    }
}
