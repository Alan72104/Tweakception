package a7.tweakception.mixin;

import com.google.common.collect.Ordering;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiPlayerTabOverlay.class)
public interface AccessorGuiPlayerTabOverlay
{
    @Accessor("field_175252_a")
    static Ordering<NetworkPlayerInfo> getTabListOrdering()
    {
        throw new AssertionError();
    }
}
