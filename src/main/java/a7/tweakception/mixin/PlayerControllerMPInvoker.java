package a7.tweakception.mixin;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerControllerMP.class)
public interface PlayerControllerMPInvoker
{
    @Invoker("syncCurrentPlayItem")
    void syncCurrentPlayItem();
}
