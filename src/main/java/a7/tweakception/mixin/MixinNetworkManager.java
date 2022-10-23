package a7.tweakception.mixin;

import a7.tweakception.events.PacketReceiveEvent;
import a7.tweakception.events.PacketSendEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager extends SimpleChannelInboundHandler<Packet<?>>
{
    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    protected void channelRead0(ChannelHandlerContext ctx, Packet<?> msg, CallbackInfo ci)
    {
        if (MinecraftForge.EVENT_BUS.post(new PacketReceiveEvent(msg)))
            ci.cancel();
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    protected void sendPacket(Packet<?> packet, CallbackInfo ci)
    {
        if (MinecraftForge.EVENT_BUS.post(new PacketSendEvent(packet)))
            ci.cancel();
    }
}
