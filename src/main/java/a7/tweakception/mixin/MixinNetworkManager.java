package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager extends SimpleChannelInboundHandler<Packet<?>>
{
    @Inject(method = "channelRead0*", at = @At("HEAD"))
    protected void channelRead0(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci)
    {
        Tweakception.globalTweaks.onPacketReceive(packet);
    }
    
    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"))
    protected void sendPacket(Packet<?> packet, CallbackInfo ci)
    {
        Tweakception.globalTweaks.onPacketSend(packet);
        Tweakception.gardenTweaks.onPacketSend(packet);
    }
}
