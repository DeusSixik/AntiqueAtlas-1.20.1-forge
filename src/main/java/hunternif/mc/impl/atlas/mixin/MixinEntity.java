package hunternif.mc.impl.atlas.mixin;

import hunternif.mc.impl.atlas.mixinhooks.EntityHooksAA;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public class MixinEntity implements EntityHooksAA {
//    @Shadow
//    protected boolean inNetherPortal;

    @Override
    public boolean antiqueAtlas_isInPortal() {
        return false;
    }
}
