package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static a7.tweakception.tweaks.GlobalTracker.isInSkyblock;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient
{
    @Inject(method = "handleBlockChange", at = @At(value="HEAD"))
    public void handleBlockChange(S23PacketBlockChange packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;

        Tweakception.miningTweaks.onPacketBlockChange(packet);
    }

    @Inject(method = "handleMultiBlockChange", at = @At(value="HEAD"))
    public void handleMultiBlockChange(S22PacketMultiBlockChange packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;

        Tweakception.miningTweaks.onPacketMultiBlockChange(packet);
    }

    @Inject(method = "handleBlockAction", at = @At(value="HEAD"))
    public void handleBlockAction(S24PacketBlockAction packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;

        Tweakception.miningTweaks.onPacketBlockAction(packet);
    }

    @Inject(method = "handleCollectItem", at = @At(value="HEAD"))
    public void handleCollectItem(S0DPacketCollectItem packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;

        Tweakception.dungeonTweaks.onPacketCollectItem(packet);
    }

    @Inject(method = "handleEntityStatus", at = @At(value="HEAD"))
    public void handleEntityStatus(S19PacketEntityStatus packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;

        Tweakception.dungeonTweaks.onPacketEntityStatus(packet);
    }
}
