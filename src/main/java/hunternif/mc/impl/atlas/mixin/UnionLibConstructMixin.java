package hunternif.mc.impl.atlas.mixin;

import com.stereowalker.unionlib.forge.PlatformHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.stereowalker.unionlib.UnionLib", remap = false)
public abstract class UnionLibConstructMixin {
    @Inject(method = "onModConstruct", at = @At("HEAD"), cancellable = true, remap = false)
    private void antiqueatlas$skipDevTestConfigs(CallbackInfo ci) {
        // UnionLib 12.0.18 registers dev-only concept/test configs here, which spam stdout every client start.
        if (PlatformHelper.isDevEnvironment()) {
            ci.cancel();
        }
    }
}
