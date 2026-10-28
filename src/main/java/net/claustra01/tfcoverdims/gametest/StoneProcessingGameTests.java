package net.claustra01.tfcoverdims.gametest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.container.KnappingContainer;
import net.dries007.tfc.util.data.KnappingType;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StoneProcessingGameTests {
    @GameTest(template = "empty", batch = "climate")
    public static void netherBrickProcessing(GameTestHelper helper) {
        assertBrickProcessing(helper, "netherrack", "minecraft:nether_brick", "minecraft:nether_bricks");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void netherrackReconstruction(GameTestHelper helper) {
        assertReconstruction(helper, "netherrack");
        assertDisabled(helper, List.of("nether_brick", "nether_bricks"));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,
            new SingleRecipeInput(stack("minecraft:netherrack")), helper.getLevel()).isEmpty(), "Netherrack must not smelt to bricks");
        assertNoCraft(helper, 2, 2, Collections.nCopies(4, stack("minecraft:nether_brick")));
        assertDerivatives(helper, "nether_bricks", "nether_brick");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void blackstoneReconstruction(GameTestHelper helper) {
        assertReconstruction(helper, "blackstone");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void blackstoneBrickProcessing(GameTestHelper helper) {
        assertBrickProcessing(helper, "blackstone", "tfcoverdims:blackstone_brick", "minecraft:polished_blackstone_bricks");
        assertDisabled(helper, List.of("polished_blackstone_bricks", "polished_blackstone_bricks_from_blackstone_stonecutting",
            "polished_blackstone_bricks_from_polished_blackstone_stonecutting"));
        assertNoStonecutting(helper, "minecraft:blackstone", "minecraft:polished_blackstone_bricks");
        assertNoStonecutting(helper, "minecraft:polished_blackstone", "minecraft:polished_blackstone_bricks");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void blackstoneDerivatives(GameTestHelper helper) {
        assertToolCraft(helper, List.of("minecraft:polished_blackstone_bricks"), "hammer", "minecraft:cracked_polished_blackstone_bricks");
        assertToolCraft(helper, List.of("tfcoverdims:blackstone_brick"), "chisel", "minecraft:polished_blackstone_button");
        assertToolCraft(helper, List.of("tfcoverdims:blackstone_brick", "tfcoverdims:blackstone_brick"), "chisel", "minecraft:polished_blackstone_pressure_plate");
        assertDisabled(helper, List.of("cracked_polished_blackstone_bricks", "polished_blackstone_button", "polished_blackstone_pressure_plate"));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,
            new SingleRecipeInput(stack("minecraft:polished_blackstone_bricks")), helper.getLevel()).isEmpty(), "Cracked bricks require a hammer");
        assertDerivatives(helper, "polished_blackstone_bricks", "polished_blackstone_brick");
        ItemStack blackstone = stack("minecraft:blackstone");
        craft(helper, List.of(blackstone.copy(), blackstone.copy(), ItemStack.EMPTY, blackstone.copy(), blackstone.copy()),
            "minecraft:polished_blackstone", 4);
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipesFor(RecipeType.STONECUTTING,
            new SingleRecipeInput(blackstone), helper.getLevel()).stream().anyMatch(recipe ->
                recipe.value().getResultItem(helper.getLevel().registryAccess()).is(Items.POLISHED_BLACKSTONE)),
            "Blackstone stonecutting must still produce polished blackstone");
        assertDerivatives(helper, "polished_blackstone", "polished_blackstone");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void blackstoneKnapping(GameTestHelper helper) {
        assertKnapping(helper, "blackstone", "metamorphic");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void endStoneProcessing(GameTestHelper helper) {
        assertBrickProcessing(helper, "end_stone", "tfcoverdims:end_stone_brick", "minecraft:end_stone_bricks");
        assertReconstruction(helper, "end_stone");
        assertDisabled(helper, List.of("end_stone_bricks", "end_stone_bricks_from_end_stone_stonecutting"));
        assertNoCraft(helper, 2, 2, Collections.nCopies(4, stack("minecraft:end_stone")));
        assertNoStonecutting(helper, "minecraft:end_stone", "minecraft:end_stone_bricks");
        assertDerivatives(helper, "end_stone_bricks", "end_stone_brick");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void endStoneKnapping(GameTestHelper helper) {
        assertKnapping(helper, "end_stone", "sedimentary");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "climate")
    public static void rockItemSizes(GameTestHelper helper) {
        for (String name : List.of("loose_netherrack", "loose_blackstone", "loose_end_stone", "blackstone_brick", "end_stone_brick")) {
            ItemStack actual = stack("tfcoverdims:" + name);
            ItemStack reference = stack(name.startsWith("loose_") ? "tfc:rock/loose/granite" : "tfc:brick/granite");
            var size = ItemSizeManager.get(actual);
            var referenceSize = ItemSizeManager.get(reference);
            helper.assertTrue(size.getSize(actual) == referenceSize.getSize(reference)
                && size.getWeight(actual) == referenceSize.getWeight(reference), "Size/weight must match TFC: " + name);
            helper.assertTrue(ItemSizeManager.MANAGER.getValues().stream().filter(definition -> definition.ingredient().test(actual)).count() == 1,
                "Every new item must have exactly one explicit size/weight definition: " + name);
        }
        helper.succeed();
    }

    private static void assertReconstruction(GameTestHelper helper, String rock) {
        ItemStack loose = stack("tfcoverdims:loose_" + rock);
        ItemStack block = craft(helper, List.of(loose.copy(), loose.copy(), ItemStack.EMPTY, loose.copy(), loose.copy()), "minecraft:" + rock, 1);
        craft(helper, List.of(block), "tfcoverdims:loose_" + rock, 4);
    }

    private static void assertBrickProcessing(GameTestHelper helper, String rock, String brick, String bricks) {
        List<ItemStack> made = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            made.add(assertToolCraft(helper, List.of("tfcoverdims:loose_" + rock), "chisel", brick));
        }
        ItemStack mortar = stack("tfc:mortar");
        craft(helper, List.of(made.get(0), mortar.copy(), made.get(1), mortar.copy(), ItemStack.EMPTY,
            mortar.copy(), made.get(2), mortar.copy(), made.get(3)), bricks, 4);
        assertNoCraft(helper, 2, 2, Collections.nCopies(4, stack(brick)));
    }

    private static ItemStack assertToolCraft(GameTestHelper helper, List<String> materials, String tool, String output) {
        List<ItemStack> inputs = new ArrayList<>();
        materials.forEach(material -> inputs.add(stack(material)));
        ItemStack toolStack = stack("tfc:metal/" + tool + "/copper");
        toolStack.setDamageValue(7);
        inputs.add(toolStack);
        return craft(helper, inputs, output, 1);
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> inputs, String output, int count) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
        for (int i = 0; i < inputs.size(); i++) {
            grid.setItem(i, inputs.get(i).copy());
        }
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "Crafting must match for " + output);
        ItemStack result = recipe.orElseThrow().value().assemble(grid.asCraftInput(), helper.getLevel().registryAccess());
        helper.assertTrue(result.is(item(output)) && result.getCount() == count, "Wrong crafting result for " + output);
        var resultContainer = new ResultContainer();
        resultContainer.setRecipeUsed(recipe.orElseThrow());
        resultContainer.setItem(0, result);
        var slot = new ResultSlot(player, grid, resultContainer, 0, 0, 0);
        ItemStack taken = slot.remove(count);
        slot.onTake(player, taken);
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack before = inputs.get(i);
            ItemStack after = grid.getItem(i);
            if (before.isDamageableItem()) {
                helper.assertTrue(after.is(before.getItem()) && after.getCount() == 1
                    && after.getDamageValue() == before.getDamageValue() + 1, "TFC crafting must consume one tool durability");
            } else {
                helper.assertTrue(after.isEmpty(), "Every brick, mortar and rock input must be consumed");
            }
        }
        return taken;
    }

    private static void assertDisabled(GameTestHelper helper, List<String> recipes) {
        for (String recipe : recipes) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.withDefaultNamespace(recipe)).isEmpty(),
                "The bypass recipe must be disabled: " + recipe);
        }
    }

    private static void assertNoCraft(GameTestHelper helper, int width, int height, List<ItemStack> inputs) {
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
            CraftingInput.of(width, height, inputs), helper.getLevel()).isEmpty(), "Crafting must not bypass mortar or tools");
    }

    private static void assertNoStonecutting(GameTestHelper helper, String input, String output) {
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipesFor(RecipeType.STONECUTTING,
            new SingleRecipeInput(stack(input)), helper.getLevel()).stream().noneMatch(recipe ->
                recipe.value().getResultItem(helper.getLevel().registryAccess()).is(item(output))), "Stonecutting must not bypass mortar");
    }

    private static void assertDerivatives(GameTestHelper helper, String input, String prefix) {
        ItemStack block = stack("minecraft:" + input);
        craft(helper, List.of(block.copy(), ItemStack.EMPTY, ItemStack.EMPTY, block.copy(), block.copy(), ItemStack.EMPTY,
            block.copy(), block.copy(), block.copy()), "minecraft:" + prefix + "_stairs", 4);
        craft(helper, List.of(block.copy(), block.copy(), block.copy()), "minecraft:" + prefix + "_slab", 6);
        craft(helper, List.of(block.copy(), block.copy(), block.copy(), block.copy(), block.copy(), block.copy()),
            "minecraft:" + prefix + "_wall", 6);
    }

    private static void assertKnapping(GameTestHelper helper, String rock, String category) {
        helper.assertTrue(BuiltInRegistries.ITEM.keySet().stream()
            .filter(id -> id.getNamespace().equals(TFCOverdims.MOD_ID)).map(ResourceLocation::getPath)
            .filter(name -> !name.startsWith("wood/")).collect(Collectors.toSet())
            .equals(Set.of("loose_netherrack", "loose_blackstone", "loose_end_stone", "blackstone_brick", "end_stone_brick")),
            "Only the five M02 items may be registered; stone tools and heads must use TFC items");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack input = new ItemStack(item("tfcoverdims:loose_" + rock), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, input);
        KnappingType type = KnappingType.get(input);
        helper.assertTrue(type != null, "Loose rock must open TFC's rock knapping");
        helper.assertTrue(KnappingType.MANAGER.getId(type).equals(ResourceLocation.fromNamespaceAndPath("tfc", "rock")),
            "Knapping must use the existing rock type");
        KnappingContainer menu = KnappingContainer.create(input, type, InteractionHand.MAIN_HAND, 0, player.getInventory(), 0);
        String pattern = " #   " + "#### " + "#####" + "#### " + " #   ";
        for (int i = 0; i < 25; i++) {
            if (pattern.charAt(i) == ' ') {
                menu.onButtonPress(i, null);
            }
        }
        ItemStack head = menu.getSlot(0).getItem().copy();
        helper.assertTrue(head.is(item("tfc:stone/axe_head/" + category)) && head.getCount() == 1,
            "Existing knapping recipe must produce the correct TFC head");
        helper.assertTrue(input.getCount() == 1, "TFC rock knapping must consume one loose rock");
        craft(helper, List.of(head, ItemStack.EMPTY, ItemStack.EMPTY, stack("minecraft:stick")), "tfc:stone/axe/" + category, 1);
    }

    private static ItemStack stack(String id) {
        return new ItemStack(item(id));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}
