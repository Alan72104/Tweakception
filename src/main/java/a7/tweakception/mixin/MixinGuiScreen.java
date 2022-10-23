package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(GuiScreen.class)
public class MixinGuiScreen
{
    @Inject(method = "renderToolTip", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void renderToolTip(ItemStack stack, int x, int y, CallbackInfo ci, List<String> list, FontRenderer r)
    {
        Tweakception.globalTweaks.updateTooltipToCopy(list);
    }
}
