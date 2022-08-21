package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.GlobalTracker;
import a7.tweakception.utils.McUtils;
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
    @Inject(method = "handleBlockChange", at = @At(value = "RETURN"))
    public void handleBlockChange(S23PacketBlockChange packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.miningTweaks.onPacketBlockChange(packet);
    }
    
    @Inject(method = "handleMultiBlockChange", at = @At(value = "RETURN"))
    public void handleMultiBlockChange(S22PacketMultiBlockChange packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.miningTweaks.onPacketMultiBlockChange(packet);
    }
    
    @Inject(method = "handleBlockAction", at = @At(value = "RETURN"))
    public void handleBlockAction(S24PacketBlockAction packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.miningTweaks.onPacketBlockAction(packet);
    }
    
    @Inject(method = "handleCollectItem", at = @At(value = "RETURN"))
    public void handleCollectItem(S0DPacketCollectItem packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.dungeonTweaks.onPacketCollectItem(packet);
        if (GlobalTracker.t)
            McUtils.sendChat("collected " + Thread.currentThread().getName());
    }
    
    @Inject(method = "handleEntityStatus", at = @At(value = "RETURN"))
    public void handleEntityStatus(S19PacketEntityStatus packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.dungeonTweaks.onPacketEntityStatus(packet);
    }
    
    @Inject(method = "handleEntityEquipment", at = @At(value = "RETURN"))
    public void handleEntityEquipment(S04PacketEntityEquipment packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.dungeonTweaks.onPacketEntityEquipment(packet);
    }
    
    @Inject(method = "handleJoinGame", at = @At(value = "RETURN"))
    public void handleJoinGame(S01PacketJoinGame packet, CallbackInfo ci)
    {
        Tweakception.globalTracker.pingReset();
    }
    
    @Inject(method = "handleStatistics", at = @At(value = "RETURN"))
    public void handleStatistics(S37PacketStatistics packet, CallbackInfo ci)
    {
        Tweakception.globalTracker.pingDone();
    }
    
    @Inject(method = "handleSetSlot", at = @At(value = "RETURN"))
    public void handleSetSlot(S2FPacketSetSlot p_handleSetSlot_1_, CallbackInfo ci)
    {
        Tweakception.enchantingTweaks.updateContents(true);
    }
}
