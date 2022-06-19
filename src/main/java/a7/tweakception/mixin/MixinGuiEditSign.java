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

@Mixin(GuiEditSign.class)
public class MixinGuiEditSign extends GuiScreen
{
    @Shadow
    private TileEntitySign tileSign;
    @Shadow
    private GuiButton doneBtn;
    private static final String[][] numberTypingSigns =
    {
        {"^^^^^^^^^^^^^^^", "Enter amount", "to order"},
        {"^^^^^^^^^^^^^^^", "Enter price", "big nerd"},
        {"^^^^^^^^^^^^^^^", "Enter amount", "to sell"},
        {"^^^^^^^^^^^^^^^", "Enter price", "per unit"},
        {"^^^^^^^^^^^^^^^", "Your auction", "starting bid"}
    };

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    protected void keyTyped(char chr, int key, CallbackInfo ci) throws IOException
    {
        if (Tweakception.globalTracker.isEnterToCloseNumberTypingSignOn() &&
            key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)
        {
            if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
                return;

            for (String[] ele : numberTypingSigns)
            {
                if (this.tileSign.signText[1].getUnformattedText().equals(ele[0]) &&
                    this.tileSign.signText[2].getUnformattedText().equals(ele[1]) &&
                    this.tileSign.signText[3].getUnformattedText().equals(ele[2]))
                {
                    this.actionPerformed(this.doneBtn);
                    ci.cancel();
                    return;
                }
            }
        }
    }
}
