package net.claustra01.tfcoverdims.mixin;

import net.neoforged.neoforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.ForgeEventHandler;

@Mixin(value = ForgeEventHandler.class, remap = false)
public abstract class NetherPortalCompatibilityMixin {
    @Inject(method = "onCreateNetherPortal", at = @At("HEAD"), cancellable = true)
    private static void allowVanillaPortal(BlockEvent.PortalSpawnEvent event, CallbackInfo ci) {
        // Skip TFC's enableNetherPortals veto so Vanilla portal rules apply.
        ci.cancel();
    }
}
