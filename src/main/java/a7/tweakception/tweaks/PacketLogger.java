package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.utils.McUtils;
import net.minecraft.network.Packet;
import net.minecraft.util.ChatComponentTranslation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static a7.tweakception.utils.McUtils.sendChat;

public class PacketLogger
{
    private final Object packetLogLock = new Object();
    private final Set<String> allowedPacketClasses = new HashSet<>();
    private boolean logPacket = false;
    private BufferedWriter packetLogWriter = null;
    private boolean logAll = false;
    
    public void toggle()
    {
        if (logPacket)
        {
            stop();
            sendChat("PacketLogger: toggled off packet log");
        }
        else
        {
            start();
        }
    }
    
    public void start()
    {
        logPacket = true;
        logAll = false;
        try
        {
            File file = Tweakception.configuration.createFileWithCurrentDateTime("packets_$_.txt");
            synchronized (packetLogLock)
            {
                packetLogWriter = Tweakception.configuration.createWriterFor(file);
            }
            McUtils.getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                McUtils.makeFileLink(file)));
            sendChat("PacketLogger: toggled on packet log");
        }
        catch (IOException e)
        {
            sendChat("PacketLogger: cannot create file");
            logPacket = false;
        }
    }
    
    public void stop()
    {
        logPacket = false;
        synchronized (packetLogLock)
        {
            try
            {
                packetLogWriter.close();
                packetLogWriter = null;
            }
            catch (IOException ignored)
            {
            }
        }
    }
    
    public void toggleAllowedPacket(String name)
    {
        if (allowedPacketClasses.contains(name))
        {
            allowedPacketClasses.remove(name);
            sendChat("PacketLogger: removed " + name);
        }
        else
        {
            allowedPacketClasses.add(name);
            sendChat("PacketLogger: added " + name);
        }
    }
    
    public void toggleLogAll()
    {
        logAll = !logAll;
        sendChat("PacketLogger: toggled log all " + logAll);
    }
    
    public void logPacket(String direction, Packet<?> packet)
    {
        if (logPacket)
        {
            synchronized (packetLogLock)
            {
                try
                {
                    String s = packet.getClass().getSimpleName();
                    if (logAll || allowedPacketClasses.contains(s))
                    {
                        packetLogWriter.write(direction);
                        packetLogWriter.write('-');
                        packetLogWriter.write(s);
                        packetLogWriter.newLine();
                        printFields(packet);
                        packetLogWriter.flush();
                    }
                }
                catch (Exception | StackOverflowError e)
                {
                    sendChat("PacketLogger: exception on logging packet!");
                    e.printStackTrace();
                    toggle();
                }
            }
        }
    }
    
    private void printFields(Object obj)
    {
        printFields(obj, obj.getClass());
    }
    
    private void printFields(Object obj, Class<?> clazz)
    {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields)
        {
            try
            {
                field.setAccessible(true);
                Object value = field.get(obj);
                
                String modifiers = Modifier.toString(field.getModifiers());
                String type = field.getType().getSimpleName();
                String name = field.getName();
                String valueString = objToString(field.getType(), value);
                write(packetLogWriter, "    ", modifiers, " ", type, " ", name, ": ", valueString);
                packetLogWriter.newLine();
            }
            catch (Exception ignored)
            {
            }
        }
        
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null)
        {
            printFields(obj, superclass);
        }
    }
    
    private String objToString(Class<?> clazz, Object obj)
    {
        if (clazz.isPrimitive())
        {
            return String.valueOf(obj);
        }
        else if (obj == null)
        {
            return "null";
        }
        
        try
        {
            if (obj.getClass().getMethod("toString").getDeclaringClass() != Object.class)
            {
                return obj.toString();
            }
            else
            {
                return "obj";
            }
        }
        catch (NoSuchMethodException ignored)
        {
        }
        
        return "NOWAY";
    }
    
    private static void write(BufferedWriter writer, String... strings) throws IOException
    {
        for (String s : strings)
            writer.append(s);
    }
}
