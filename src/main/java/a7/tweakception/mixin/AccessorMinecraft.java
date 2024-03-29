package a7.tweakception.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface AccessorMinecraft
{
    @Accessor("timer")
    Timer getTimer();
    
    @Invoker("rightClickMouse")
    void rightClickMouse();
    
    @Invoker("clickMouse")
    void clickMouse();
}
