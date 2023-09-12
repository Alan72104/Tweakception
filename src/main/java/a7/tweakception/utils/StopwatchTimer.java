package a7.tweakception.utils;

public class StopwatchTimer
{
    public int period;
    public long startMillis = 0;
    
    public StopwatchTimer(int period)
    {
        this(period, false);
    }
    
    public StopwatchTimer(int period, boolean start)
    {
        this.period = period;
        if (start)
            start();
    }
    
    public void stop()
    {
        startMillis = 0;
    }
    
    public void start()
    {
        startMillis = System.currentTimeMillis();
    }
    
    public boolean isRunning()
    {
        return startMillis > 0 && System.currentTimeMillis() - startMillis < period;
    }
    
    public long elapsedTime()
    {
        return System.currentTimeMillis() - startMillis;
    }
}
