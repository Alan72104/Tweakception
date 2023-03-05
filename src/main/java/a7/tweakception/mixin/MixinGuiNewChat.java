package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat
{
    @Final
    @Shadow
    private static Logger logger;
    
//    @Redirect(method = "printChatMessageWithOptionalDeletion",
    @Inject(method = "printChatMessageWithOptionalDeletion", cancellable = true,
        at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/GuiNewChat;setChatLine(Lnet/minecraft/util/IChatComponent;IIZ)V")
//        at = @At(value = "INVOKE", opcode = Opcodes.INVOKEINTERFACE,
//            target = "Lnet/minecraft/util/IChatComponent;getUnformattedText()Ljava/lang/String;"),
//        slice = @Slice(
//            from = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;append(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
//            to = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;toString()Ljava/lang/String;")
//        )
    )
    private void redirectChatLoggingGetString(IChatComponent chatComponent, int lineId, CallbackInfo ci)
    {
        if (Tweakception.globalTweaks.isChatLogForceFormattedOn())
        {
            logger.info("[CHAT] " + chatComponent.getFormattedText());
            ci.cancel();
        }
        
//        if (Tweakception.globalTweaks.isCatLogForceFormattedOn())
//            return chatComponent.getFormattedText();
//        else
//            return chatComponent.getUnformattedText();
    }
}
