package a7.tweakception;

import a7.tweakception.utils.McUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static a7.tweakception.utils.Utils.f;

public class LagSpikeWatcher
{
    private static LagSpikeWatcherThread watcherThread;
    private static volatile int threshold = 250;
    private static boolean keepLoggingOnLag = false;
    private static final String newline = System.lineSeparator();
    
    public static void startWatcher()
    {
        if (!isWatcherOn())
        {
            watcherThread = new LagSpikeWatcherThread(Thread.currentThread());
            watcherThread.start();
        }
    }
    
    public static void stopWatcher()
    {
        if (watcherThread != null)
        {
            watcherThread.exit();
            try
            {
                watcherThread.join();
            }
            catch (InterruptedException ignored)
            {
            }
            watcherThread = null;
        }
    }
    
    public static boolean isWatcherOn()
    {
        return watcherThread != null && !watcherThread.exit && watcherThread.isAlive();
    }
    
    public static void newTick()
    {
        if (watcherThread != null)
            watcherThread.newTick();
    }
    
    public static int setThreshold(int i)
    {
        return threshold = i <= 0 ? 500 : i;
    }
    
    public static boolean toggleKeepLoggingOnLag()
    {
        return keepLoggingOnLag = !keepLoggingOnLag;
    }
    
    public static File dump()
    {
        if (watcherThread != null)
        {
            Set<Map.Entry<String, Integer>> set;
            synchronized (watcherThread.lagSources)
            {
                set = watcherThread.lagSources.entrySet();
            }
            List<Map.Entry<String, Integer>> sorted = set.stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
            
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sorted)
            {
                lines.add("Stack detected " + entry.getValue() + " times");
                lines.add("==================================================");
                lines.add(entry.getKey());
                lines.add("==================================================");
                lines.add(newline);
            }
            
            try
            {
                return Tweakception.configuration.createWriteFileWithCurrentDateTime("lagsources_$.txt", lines);
            }
            catch (IOException e)
            {
                return null;
            }
        }
        return null;
    }
    
    public static File dumpThreads()
    {
        List<String> lines = new ArrayList<>();
        
        List<Map.Entry<Thread, StackTraceElement[]>> list = Thread.getAllStackTraces()
            .entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
            .collect(Collectors.toList());
        
        lines.add(f("%-35s |%-15s |%-10s |%s", "Name", "State", "Priority", "IsDaemon"));
        // Java stream is shit
//        for (Thread t : list.stream().collect(ArrayList<Thread>::new, (a, e) -> a.add(e.getKey()), ArrayList::addAll))
//            lines.add(f("%-35s |%-15s |%-10d |%s", t.getName(), t.getState(), t.getPriority(), t.isDaemon()));
        for (Map.Entry<Thread, StackTraceElement[]> e : list)
        {
            Thread t = e.getKey();
            lines.add(f("%-35s |%-15s |%-10d |%s", t.getName(), t.getState(), t.getPriority(), t.isDaemon()));
        }
        
        lines.add("");
        lines.add("All stacks");
        lines.add("");
        for (Map.Entry<Thread, StackTraceElement[]> e : list)
        {
            lines.add("Thread " + e.getKey().getName());
            lines.add("==================================================");
            for (StackTraceElement ele : e.getValue())
                lines.add(stackFrameToString(ele));
            lines.add("==================================================");
            lines.add("");
        }
        
        try
        {
            return Tweakception.configuration.createWriteFileWithCurrentDateTime("allthreaddump_$.txt", lines);
        }
        catch (IOException e)
        {
            return null;
        }
    }
    
    private static String stackFrameToString(StackTraceElement ele)
    {
        return f("%s.%s(%s:%d)", ele.getClassName(), ele.getMethodName(), ele.getFileName(), ele.getLineNumber());
    }
    
    private static class LagSpikeWatcherThread extends Thread
    {
        private boolean exit = false;
        private volatile long tickStartMillis = 0L;
        private volatile boolean newTick = false;
        private final Object newTickMonitor = new Object();
        private final Thread mainThread;
        private final Map<String, Integer> lagSources = new HashMap<>();
        
        public LagSpikeWatcherThread(Thread mainThread)
        {
            setName("LagSpikeWatcherThread");
            setDaemon(true);
            this.mainThread = mainThread;
        }
        
        @Override
        public void run()
        {
            while (!exit)
            {
                synchronized (newTickMonitor)
                {
                    try
                    {
                        newTickMonitor.wait(threshold);
                    }
                    catch (InterruptedException ignored)
                    {
                        break;
                    }
                    tickedOrTimedOut();
                    newTick = false;
                }
            }
        }
        
        private void tickedOrTimedOut()
        {
            if (System.currentTimeMillis() - tickStartMillis >= threshold && (keepLoggingOnLag || newTick))
            {
                StackTraceElement[] stack = mainThread.getStackTrace();
                StringBuilder stackStringBuilder = new StringBuilder();
                
                Tweakception.logger.debug("==================================================");
                Tweakception.logger.debug("LagSpikeWatcher: this tick has lasted >" + threshold + "ms since start");
                for (StackTraceElement ele : stack)
                {
                    String at = stackFrameToString(ele);
                    
                    Tweakception.logger.debug("at " + at);
                    stackStringBuilder.append(at);
                    
                    if (ele.getClassName().equals("net.minecraft.client.main.Main") &&
                        ele.getMethodName().equals("main"))
                    {
                        Tweakception.logger.debug("rest omitted...");
                        break;
                    }
                    else
                        stackStringBuilder.append(newline);
                }
                Tweakception.logger.debug("==================================================");
                
                String stackString = stackStringBuilder.toString();
                synchronized (lagSources)
                {
                    if (lagSources.containsKey(stackString))
                        lagSources.merge(stackString, 1, Integer::sum);
                    else
                        lagSources.put(stackString, 1);
                }
                
                Tweakception.scheduler.add(() ->
                {
                    if (McUtils.isInGame())
                    {
                        McUtils.sendChat("LagSpikeWatcher: this tick has lasted >" + threshold + "ms since start");
                        McUtils.sendChat("stack top -> " + stack[0].getMethodName());
                    }
                });
            }
        }
        
        public void exit()
        {
            exit = true;
            interrupt();
        }
        
        public void newTick()
        {
            synchronized (newTickMonitor)
            {
                tickStartMillis = System.currentTimeMillis();
                newTick = true;
                newTickMonitor.notify();
            }
        }
    }
}
