package net.claustra01.tfcoverdims.gametest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import net.claustra01.tfcoverdims.TFCOverdims;
import net.claustra01.tfcoverdims.common.EnchantingLapis;
import net.dries007.tfc.common.component.forge.ForgingBonus;
import net.dries007.tfc.common.component.forge.ForgingBonusComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnchantingGameTests {
    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @GameTest(template = "empty", batch = "climate")
    public static void lapisEnchanting(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.ENCHANTING_TABLE.defaultBlockState());
        player.experienceLevel = 30;
        for (String id : new String[] {"minecraft:lapis_lazuli", "tfc:gem/lapis_lazuli"}) {
            var menu = new EnchantmentMenu(1, player.getInventory(), ContainerLevelAccess.create(level, pos));
            var lapis = stack(id);
            lapis.setCount(3);
            helper.assertTrue(menu.slots.get(1).mayPlace(lapis), "Lapis slot accepts " + id);
            helper.assertTrue(menu.slots.get(1).x == 35 && menu.slots.get(1).y == 47,
                "Vanilla lapis slot position is retained");
            helper.assertTrue(menu.slots.get(1).getNoItemIcon().getSecond()
                .equals(ResourceLocation.withDefaultNamespace("item/empty_slot_lapis_lazuli")),
                "Vanilla empty slot icon is retained");
            menu.slots.get(0).set(new ItemStack(Items.IRON_SWORD));
            menu.slots.get(1).set(lapis);
            helper.assertTrue(menu.costs[0] > 0, "Enchant offer exists for " + id);
            helper.assertTrue(menu.clickMenuButton(player, 0), "Enchant succeeds with " + id);
            helper.assertTrue(menu.slots.get(1).getItem().getCount() == 2, "One lapis consumed for " + id);
            helper.assertTrue(!EnchantmentHelper.getEnchantmentsForCrafting(menu.slots.get(0).getItem()).isEmpty(),
                "Enchant applied with " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void lapisQuickMove(GameTestHelper helper) throws ReflectiveOperationException {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        Field tags = Holder.Reference.class.getDeclaredField("tags");
        tags.setAccessible(true);
        for (String id : new String[] {"minecraft:lapis_lazuli", "tfc:gem/lapis_lazuli"}) {
            var menu = new EnchantmentMenu(1, player.getInventory());
            var lapis = stack(id);
            lapis.setCount(3);
            var item = lapis.getItem();
            player.getInventory().setItem(9, lapis);
            helper.assertTrue(menu.quickMoveStack(player, 2).is(item), "Shift move accepts " + id);
            helper.assertTrue(menu.slots.get(1).getItem().is(item), "Lapis reaches slot 1");
            helper.assertTrue(menu.slots.get(0).getItem().isEmpty(), "Lapis does not reach equipment slot");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "Source slot emptied");
            var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
            Object originalTags = tags.get(holder);
            try {
                tags.set(holder, Set.of());
                helper.assertTrue(!stack(id).is(EnchantingLapis.TAG), "Lapis tag is unavailable for " + id);
                helper.assertTrue(menu.slots.get(1).mayPlace(stack(id)), "Slot fallback accepts " + id);
                menu.slots.get(1).set(ItemStack.EMPTY);
                player.getInventory().setItem(9, stack(id));
                helper.assertTrue(menu.quickMoveStack(player, 2).is(item), "Shift fallback accepts " + id);
                helper.assertTrue(menu.slots.get(1).getItem().is(item), "Shift fallback targets lapis slot");
                helper.assertTrue(menu.slots.get(0).getItem().isEmpty(), "Shift fallback avoids equipment slot");
            } finally {
                tags.set(holder, originalTags);
            }
        }
        helper.assertTrue(stack("minecraft:lapis_lazuli").is(EnchantingLapis.TAG)
            && stack("tfc:gem/lapis_lazuli").is(EnchantingLapis.TAG), "Both lapis items are tagged");
        helper.assertTrue(!EnchantingLapis.accepts(new ItemStack(Items.DIAMOND)), "Other gems are rejected");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void tfcEquipmentEnchanting(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var sword = stack("tfc:metal/sword/steel");
        var pickaxe = stack("tfc:metal/pickaxe/steel");
        var chestplate = stack("tfc:metal/chestplate/steel");
        helper.assertTrue(sword.is(ItemTags.SWORD_ENCHANTABLE) && sword.isEnchantable(), "Steel sword is enchantable");
        helper.assertTrue(pickaxe.is(ItemTags.MINING_ENCHANTABLE) && pickaxe.isEnchantable(), "Steel pickaxe is enchantable");
        helper.assertTrue(chestplate.is(ItemTags.CHEST_ARMOR_ENCHANTABLE) && chestplate.isEnchantable(),
            "Steel chestplate is enchantable");
        ForgingBonusComponent.set(pickaxe, ForgingBonus.PERFECT);
        var equipment = new ItemStack[] {sword, pickaxe, chestplate};
        var enchantments = List.of(Enchantments.SHARPNESS, Enchantments.UNBREAKING,
            Enchantments.PROTECTION);
        for (int i = 0; i < equipment.length; i++) {
            var enchantment = registry.getHolderOrThrow(enchantments.get(i));
            var menu = new AnvilMenu(1, player.getInventory());
            menu.slots.get(0).set(equipment[i]);
            menu.slots.get(1).set(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, 1)));
            var result = menu.slots.get(2).getItem();
            helper.assertTrue(!result.isEmpty(), "Anvil accepts finished steel equipment " + i);
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(enchantment, result) == 1,
                "Vanilla enchant is retained on steel equipment " + i);
            if (i == 0) {
                helper.assertTrue(EnchantmentHelper.modifyDamage(level, result, player,
                    level.damageSources().playerAttack(player), 5) > 5, "Sharpness increases steel sword damage");
            }
            if (i == 1) {
                helper.assertTrue(ForgingBonusComponent.get(result) == ForgingBonus.PERFECT,
                    "Forging bonus survives anvil enchantment");
                helper.assertTrue(result.has(DataComponents.MAX_DAMAGE), "Enchanted tool remains damageable");
                helper.assertTrue(EnchantmentHelper.processDurabilityChange(level, result, 100) < 100,
                    "Unbreaking reduces durability use");
                helper.assertTrue(ForgingBonusComponent.applyLikeUnbreaking(result, RandomSource.create(1), 100) < 100,
                    "Forging bonus reduces durability use");
            }
            if (i == 2) {
                player.setItemSlot(EquipmentSlot.CHEST, result);
                helper.assertTrue(EnchantmentHelper.getDamageProtection(level, player,
                    level.damageSources().generic()) > 0, "Protection reduces damage with steel chestplate");
            }
        }
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.ENCHANTING_TABLE.defaultBlockState());
        var table = new EnchantmentMenu(2, player.getInventory(), ContainerLevelAccess.create(level, pos));
        player.experienceLevel = 30;
        table.slots.get(0).set(stack("tfc:metal/sword/steel"));
        table.slots.get(1).set(stack("tfc:gem/lapis_lazuli"));
        helper.assertTrue(table.costs[0] > 0 && table.clickMenuButton(player, 0),
            "Enchantment table enchants finished steel sword");
        helper.assertTrue(!EnchantmentHelper.getEnchantmentsForCrafting(table.slots.get(0).getItem()).isEmpty(),
            "Table enchant is retained by steel sword");
        for (String id : new String[] {"tfc:metal/pickaxe_head/steel", "tfc:metal/sheet/steel"}) {
            var unfinished = stack(id);
            helper.assertTrue(!unfinished.isEnchantable() && !unfinished.is(ItemTags.MINING_ENCHANTABLE),
                "Unfinished component remains ineligible: " + id);
            var menu = new AnvilMenu(1, player.getInventory());
            menu.slots.get(0).set(unfinished);
            menu.slots.get(1).set(EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(registry.getHolderOrThrow(Enchantments.UNBREAKING), 1)));
            helper.assertTrue(menu.slots.get(2).getItem().isEmpty(), "Anvil rejects unfinished component: " + id);
        }
        helper.assertTrue(BuiltInRegistries.MENU.keySet().stream().noneMatch(id -> id.getNamespace().equals(TFCOverdims.MOD_ID)),
            "No enchanting menu is registered");
        helper.assertTrue(registry.keySet().stream().noneMatch(id -> id.getNamespace().equals(TFCOverdims.MOD_ID)),
            "No enchantment is registered");
        helper.succeed();
    }
}
