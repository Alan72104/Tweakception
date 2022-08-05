package a7.tweakception;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Scheduler
{
    private final Queue<ScheduledTask> tasks = new ConcurrentLinkedQueue<>();
    private int ticks = 0;
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        ticks++;
        if (!tasks.isEmpty())
        {
            Iterator<ScheduledTask> it = tasks.iterator();
            while (it.hasNext())
            {
                ScheduledTask task = it.next();
                if (ticks - task.startTicks >= task.delay)
                {
                    task.task.run();
                    if (task.andThen != null)
                        task.andThen.run();
                    it.remove();
                }
            }
        }
    }
    
    public ScheduledTask add(Runnable task)
    {
        ScheduledTask scheduledTask = new ScheduledTask(this, task, ticks, 0);
        tasks.add(scheduledTask);
        return scheduledTask;
    }
    
    public ScheduledTask addDelayed(Runnable task, int delayTicks)
    {
        ScheduledTask scheduledTask = new ScheduledTask(this, task, ticks, delayTicks);
        tasks.add(scheduledTask);
        return scheduledTask;
    }
    
    public void remove(ScheduledTask task)
    {
        tasks.remove(task);
    }
    
    public static class ScheduledTask
    {
        private final Scheduler scheduler;
        public Runnable task;
        public Runnable andThen;
        public int startTicks;
        public int delay;
        
        public ScheduledTask(Scheduler scheduler, Runnable task, int startTicks, int delay)
        {
            this.scheduler = scheduler;
            this.task = task;
            this.startTicks = startTicks;
            if (delay < 0)
                delay = 0;
            this.delay = delay;
        }
        
        public ScheduledTask then(Runnable task)
        {
            andThen = () -> scheduler.add(task);
            return this;
        }
        
        public ScheduledTask thenDelayed(Runnable task, int delayTicks)
        {
            andThen = () -> scheduler.addDelayed(task, delayTicks);
            return this;
        }
    }
}
