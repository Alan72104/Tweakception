package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(GuiEditSign.class)
public class MixinGuiEditSign extends GuiScreen
{
    @Shadow
    private TileEntitySign tileSign;
    @Shadow
    private GuiButton doneBtn;
    private static final Matcher onlyCaretMatcher = Pattern.compile("^\\^+$").matcher("");
    
    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    protected void keyTyped(char chr, int key, CallbackInfo ci) throws IOException
    {
        if (Tweakception.globalTweaks.isEnterToCloseNumberTypingSignOn() &&
            key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)
        {
            if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
                return;
            
            if (onlyCaretMatcher.reset(this.tileSign.signText[1].getUnformattedText()).matches() ||
                onlyCaretMatcher.reset(this.tileSign.signText[2].getUnformattedText()).matches())
            {
                this.actionPerformed(this.doneBtn);
                ci.cancel();
            }
        }
    }
}
