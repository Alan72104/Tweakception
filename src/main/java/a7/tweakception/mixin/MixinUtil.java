package a7.tweakception.mixin;

import net.minecraft.util.Util;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.FutureTask;

@Mixin(Util.class)
public class MixinUtil
{
    /**
     * To get rid of the stupid try catch printStacktrace
     * @author Alan72104
     */
    @Overwrite
    public static <V> V runTask(FutureTask<V> task, Logger logger)
    {
        try
        {
            task.run();
            return task.get();
        }
        catch (Exception ignored)
        {
        }
        return null;
    }
}
