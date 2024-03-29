package a7.tweakception.utils.timers;

public class Stopwatch
{
    public int interval;
    public long lastTickMillis;
    
    public Stopwatch(int intervalMs)
    {
        this.interval = intervalMs;
        reset();
    }
    
    public void reset()
    {
        lastTickMillis = System.currentTimeMillis();
    }
    
    public long elapsedTime()
    {
        return System.currentTimeMillis() - lastTickMillis;
    }
    
    public boolean elapsed()
    {
        return System.currentTimeMillis() - lastTickMillis >= interval;
    }
    
    public boolean checkAndReset()
    {
        long cur = System.currentTimeMillis();
        boolean elapsed = cur - lastTickMillis >= interval;
        lastTickMillis = cur;
        return elapsed;
    }
    
    public boolean checkAndResetIfElapsed()
    {
        long cur = System.currentTimeMillis();
        boolean elapsed = cur - lastTickMillis >= interval;
        if (elapsed)
            lastTickMillis = cur;
        return elapsed;
    }
}
