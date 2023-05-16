package a7.tweakception;

import a7.tweakception.utils.McUtils;
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
        if (event.phase != TickEvent.Phase.START)
            return;
        
        ticks++;
        if (!tasks.isEmpty())
        {
            Iterator<ScheduledTask> it = tasks.iterator();
            while (it.hasNext())
            {
                ScheduledTask scheduledTask = it.next();
                if (ticks - scheduledTask.startTicks >= scheduledTask.delay)
                {
                    scheduledTask.task.run();
                    
                    if (DevSettings.printSchedulerUpdate)
                    {
                        McUtils.sendChatf("ScheduledTask %s completed",
                            Integer.toHexString(scheduledTask.hashCode()));
                    }
                    
                    if (scheduledTask.andThen != null)
                    {
                        scheduledTask.andThen.run();
                        
                        if (DevSettings.printSchedulerUpdate)
                        {
                            McUtils.sendChatf("ScheduledTask %s andThen completed",
                                Integer.toHexString(scheduledTask.hashCode()));
                        }
                    }
                    it.remove();
                }
            }
        }
    }
    
    public ScheduledTask add(Runnable task)
    {
        ScheduledTask scheduledTask = new ScheduledTask(this, task, ticks, 0);
        tasks.add(scheduledTask);
        if (DevSettings.printSchedulerUpdate)
        {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length >= 3)
            {
                StackTraceElement caller = stackTrace[2];
                McUtils.sendChatf("ScheduledTask %s added by %s.%s()",
                    Integer.toHexString(scheduledTask.hashCode()),
                    caller.getClassName(),
                    caller.getMethodName());
            }
        }
        return scheduledTask;
    }
    
    public ScheduledTask addDelayed(Runnable task, int delayTicks)
    {
        ScheduledTask scheduledTask = new ScheduledTask(this, task, ticks, delayTicks);
        tasks.add(scheduledTask);
        if (DevSettings.printSchedulerUpdate)
        {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length >= 3)
            {
                StackTraceElement caller = stackTrace[2];
                McUtils.sendChatf("ScheduledTask %s with delay %d added by %s.%s()",
                    Integer.toHexString(scheduledTask.hashCode()),
                    delayTicks,
                    caller.getClassName(),
                    caller.getMethodName());
            }
        }
        return scheduledTask;
    }
    
    public void remove(ScheduledTask scheduledTask)
    {
        tasks.remove(scheduledTask);
        if (DevSettings.printSchedulerUpdate)
        {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length >= 3)
            {
                StackTraceElement caller = stackTrace[2];
                if (scheduledTask.delay > 0)
                    McUtils.sendChatf("ScheduledTask %s with delay %d removed by %s.%s()",
                        Integer.toHexString(scheduledTask.hashCode()),
                        scheduledTask.delay,
                        caller.getClassName(),
                        caller.getMethodName());
                else
                    McUtils.sendChatf("ScheduledTask %s removed by %s.%s()",
                        Integer.toHexString(scheduledTask.hashCode()),
                        caller.getClassName(),
                        caller.getMethodName());
            }
        }
    }
    
    public static class ScheduledTask
    {
        private final Scheduler scheduler;
        public Runnable task;
        public Runnable andThen;
        public int startTicks;
        public int delay;
        
        private ScheduledTask(Scheduler scheduler, Runnable task, int startTicks, int delay)
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
