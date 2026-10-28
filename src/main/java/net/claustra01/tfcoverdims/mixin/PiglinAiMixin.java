package net.claustra01.tfcoverdims.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {
    @Unique
    private static final TagKey<Item> tfcoverdims$currency = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("tfcoverdims", "piglin_currency"));

    @Redirect(method = {"canAdmire", "wantsToPickup", "stopHoldingOffHandItem"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean acceptCurrency(ItemStack stack) {
        return stack.isPiglinCurrency() || stack.is(tfcoverdims$currency);
    }
}
