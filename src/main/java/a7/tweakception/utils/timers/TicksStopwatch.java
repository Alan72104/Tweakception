package a7.tweakception.utils.timers;

import static a7.tweakception.tweaks.GlobalTweaks.getTicks;

public class TicksStopwatch
{
    public int interval;
    public int lastTickTicks;
    
    public TicksStopwatch(int interval)
    {
        this.interval = interval;
        reset();
    }
    
    public void reset()
    {
        lastTickTicks = getTicks();
    }
    
    public int elapsedTime()
    {
        return getTicks() - lastTickTicks;
    }
    
    public boolean elapsed()
    {
        return getTicks() - lastTickTicks >= interval;
    }
    
    public boolean checkAndReset()
    {
        int cur = getTicks();
        boolean elapsed = cur - lastTickTicks >= interval;
        lastTickTicks = cur;
        return elapsed;
    }
    
    public boolean checkAndResetIfElapsed()
    {
        int cur = getTicks();
        boolean elapsed = cur - lastTickTicks >= interval;
        if (elapsed)
            lastTickTicks = cur;
        return elapsed;
    }
}
