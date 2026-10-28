package net.claustra01.tfcoverdims.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.claustra01.tfcoverdims.TFCOverdims;

@GameTestHolder(TFCOverdims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortalGameTests {
    @GameTest(template = "nether_tree", batch = "climate")
    public static void vanillaPortals(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        BlockPos base = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 5; y++) {
                level.setBlockAndUpdate(base.offset(x, y, 0),
                    (x == 0 || x == 3 || y == 0 || y == 4 ? Blocks.OBSIDIAN : Blocks.AIR).defaultBlockState());
            }
        }
        BlockPos missingFrame = base.offset(0, 2, 0);
        level.setBlockAndUpdate(missingFrame, Blocks.AIR.defaultBlockState());
        helper.assertTrue(PortalShape.findEmptyPortalShape(
            level, base.offset(1, 1, 0), Direction.Axis.X).isEmpty(), "An incomplete Obsidian frame must remain invalid");
        level.setBlockAndUpdate(missingFrame, Blocks.OBSIDIAN.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(base.offset(1, 0, 0)), Direction.UP, base.offset(1, 0, 0), false)));
        BlockPos portal = base.offset(1, 1, 0);
        helper.assertTrue(level.getBlockState(portal).is(Blocks.NETHER_PORTAL), "Flint and Steel must light an ordinary Obsidian frame");
        var pig = EntityType.PIG.create(level);
        pig.setPos(Vec3.atBottomCenterOf(portal));
        level.addFreshEntity(pig);
        var destination = ((NetherPortalBlock) Blocks.NETHER_PORTAL).getPortalDestination(level, pig, portal);
        helper.assertTrue(destination != null && destination.newLevel().dimension() == Level.NETHER,
            "Vanilla Nether Portal must resolve the Nether destination");
        var transferred = pig.changeDimension(destination);
        helper.assertTrue(transferred != null && transferred.level().dimension() == Level.NETHER,
            "Entities must travel through the Vanilla Nether transition");
        transferred.discard();

        BlockPos center = helper.absolutePos(new BlockPos(11, 2, 11));
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos frame = center.relative(side, 2).relative(side.getClockWise(), offset);
                level.setBlockAndUpdate(frame, Blocks.END_PORTAL_FRAME.defaultBlockState()
                    .setValue(EndPortalFrameBlock.FACING, side.getOpposite()));
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_EYE));
                player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(frame), Direction.UP, frame, false)));
                helper.assertTrue(level.getBlockState(frame).getValue(EndPortalFrameBlock.HAS_EYE),
                    "Vanilla Eye of Ender must fill each inward-facing frame");
                helper.assertTrue(player.getMainHandItem().isEmpty(), "Each frame must consume one Eye of Ender");
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            helper.assertTrue(level.getBlockState(pos).is(Blocks.END_PORTAL), "Twelve Eyes must activate the Vanilla 3x3 End Portal");
        }
        var endPig = EntityType.PIG.create(level);
        endPig.setPos(Vec3.atCenterOf(center));
        level.addFreshEntity(endPig);
        var endDestination = ((EndPortalBlock) Blocks.END_PORTAL).getPortalDestination(level, endPig, center);
        var inEnd = endPig.changeDimension(endDestination);
        helper.assertTrue(inEnd != null && inEnd.level().dimension() == Level.END, "Vanilla End Portal must transfer entities to the End");
        inEnd.discard();
        helper.succeed();
    }
}
