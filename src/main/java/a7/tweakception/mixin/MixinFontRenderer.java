package a7.tweakception.mixin;

import net.minecraft.client.gui.FontRenderer;
import a7.tweakception.tweaks.StringReplace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(FontRenderer.class)
public class MixinFontRenderer
{
    @ModifyVariable(method = "renderString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), index = 1, argsOnly = true)
    private String renderString(String s)
    {
        if (StringReplace.isOn())
            return StringReplace.replaceString(s);
        return s;
    }
    
    @ModifyVariable(method = "getStringWidth(Ljava/lang/String;)I", at = @At("HEAD"), index = 1, argsOnly = true)
    private String getStringWidth(String s)
    {
        if (StringReplace.isOn())
            return StringReplace.replaceString(s);
        return s;
    }
}
