package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

import static a7.tweakception.tweaks.GlobalTweaks.isInSkyblock;

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
    }
    
    @Inject(method = "handleCollectItem", at = @At(value = "RETURN"))
    public void handleCollectItem(S0DPacketCollectItem packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.dungeonTweaks.onPacketCollectItem(packet);
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
        Tweakception.globalTweaks.pingReset();
    }
    
    @Inject(method = "handleStatistics", at = @At(value = "RETURN"))
    public void handleStatistics(S37PacketStatistics packet, CallbackInfo ci)
    {
        Tweakception.globalTweaks.pingDone();
    }
    
    @Inject(method = "handleSetSlot", at = @At(value = "RETURN"))
    public void handleSetSlot(S2FPacketSetSlot packet, CallbackInfo ci)
    {
    }
    
    @Inject(method = "handleSoundEffect", at = @At(value = "RETURN"))
    public void handleSoundEffect(S29PacketSoundEffect packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.globalTweaks.onPacketSoundEffect(packet);
    }
    
    @Inject(method = "handlePlayerListItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/network/NetworkPlayerInfo;setDisplayName(Lnet/minecraft/util/IChatComponent;)V"),
        locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    public void handlePlayerListItem_UpdateDisplayName(S38PacketPlayerListItem packetIn,
                                                       CallbackInfo ci,
                                                       Iterator<S38PacketPlayerListItem.AddPlayerData> it,
                                                       S38PacketPlayerListItem.AddPlayerData addPlayerData,
                                                       NetworkPlayerInfo networkPlayerInfo)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.gardenTweaks.onPlayerListItemUpdateDisplayName(addPlayerData, networkPlayerInfo);
    }
    
    @Inject(method = "handleChunkData", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/WorldClient;doPreChunk(IIZ)V", ordinal = 0),
        cancellable = true)
    public void handleChunkData_UnloadChunk(S21PacketChunkData packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.globalTweaks.onPacketChunkUnload(packet, ci);
    }
    
    @Inject(method = "handleParticles", at = @At(value = "RETURN"))
    public void handleParticles(S2APacketParticles packet, CallbackInfo ci)
    {
        if (!isInSkyblock()) return;
        
        Tweakception.miningTweaks.onPacketParticles(packet);
    }
}
