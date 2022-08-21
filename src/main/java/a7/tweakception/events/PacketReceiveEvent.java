package a7.tweakception.events;

import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PacketReceiveEvent extends Event
{
    private final Packet<?> packet;
    
    public PacketReceiveEvent(Packet<?> packet)
    {
        this.packet = packet;
    }
    
    public Packet<?> getPacket()
    {
        return packet;
    }
}
