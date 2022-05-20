package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S24PacketBlockAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static a7.tweakception.tweaks.GlobalTracker.isInSkyblock;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient
{
    @Inject(method = "handleBlockChange", at = @At(value="HEAD"))
    public void handleBlockChange(S23PacketBlockChange packetIn, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;

        Tweakception.miningTweaks.onPacketBlockChange(packetIn);
    }

    @Inject(method = "handleMultiBlockChange", at = @At(value="HEAD"))
    public void handleMultiBlockChange(S22PacketMultiBlockChange packetIn, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;

        Tweakception.miningTweaks.onPacketMultiBlockChange(packetIn);
    }

    @Inject(method = "handleBlockAction", at = @At(value="HEAD"))
    public void handleBlockAction(S24PacketBlockAction packetIn, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;

        Tweakception.miningTweaks.onPacketBlockAction(packetIn);
    }

    @Inject(method = "handleCollectItem", at = @At(value="HEAD"))
    public void handleCollectItem(S0DPacketCollectItem packetIn, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;

        Tweakception.dungeonTweaks.onPacketCollectItem(packetIn);
    }
}
