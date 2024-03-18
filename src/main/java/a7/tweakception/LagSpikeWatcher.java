package a7.tweakception;

import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.timers.Stopwatch;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static a7.tweakception.utils.Utils.f;

public class LagSpikeWatcher
{
    private static LagSpikeWatcherThread watcherThread = null;
    private static volatile int threshold = 150;
    private static volatile boolean keepLoggingOnLag = false;
    private static final String newline = System.lineSeparator();
    private static final Stopwatch frameTimeGatheringStopwatch = new Stopwatch(5000);
    private static Int2IntArrayMap frameTimeMap = null;
    private static long lastTickMillis = 0;
    
    public static void startWatcher()
    {
        if (!isWatcherOn())
        {
            frameTimeGatheringStopwatch.reset();
            frameTimeMap = new Int2IntArrayMap();
            lastTickMillis = 0;
            watcherThread = new LagSpikeWatcherThread(Thread.currentThread());
        }
    }
    
    public static void startWatcherWithoutThresholdDetection()
    {
        if (!isWatcherOn())
        {
            watcherThread = new LagSpikeWatcherThread(Thread.currentThread());
            watcherThread.start();
        }
    }
    
    public static void stopWatcher()
    {
        frameTimeMap = null;
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
        return (watcherThread != null && !watcherThread.exit && watcherThread.isAlive()) || frameTimeMap != null;
    }
    
    public static void newTick()
    {
        if (frameTimeMap != null)
        {
            if (lastTickMillis == 0)
            {
                lastTickMillis = System.currentTimeMillis();
            }
            else if (frameTimeGatheringStopwatch.elapsed())
            {
                int min = frameTimeMap.int2IntEntrySet().stream()
                    .mapToInt(Int2IntMap.Entry::getIntKey)
                    .min().orElse(0);
                int max = frameTimeMap.int2IntEntrySet().stream()
                    .mapToInt(Int2IntMap.Entry::getIntKey)
                    .max().orElse(0);
                int median = findMedian(frameTimeMap);
                McUtils.sendChatf("Threshold detection done");
                McUtils.sendChatf("Min: %d ms", min);
                McUtils.sendChatf(">> Median: %d ms", median);
                McUtils.sendChatf("Max: %d ms", max);
                McUtils.sendChatf("Threshold set to %d", median * 2);
                setThreshold(median * 2);
                frameTimeMap = null;
                watcherThread.start();
            }
            else
            {
                long frameTime = System.currentTimeMillis() - lastTickMillis;
                lastTickMillis = System.currentTimeMillis();
                frameTimeMap.mergeInt((int) frameTime, 1, Integer::sum);
            }
        }
        else if (isWatcherOn())
            watcherThread.newTick();
    }
    
    private static int findMedian(Int2IntArrayMap map)
    {
        int count = map.int2IntEntrySet().stream()
            .mapToInt(Int2IntMap.Entry::getIntValue)
            .sum();
        
        Iterator<Int2IntMap.Entry> it = map.int2IntEntrySet().stream()
            .sorted(Comparator.comparingInt(Int2IntMap.Entry::getIntKey))
            .iterator();
        
        if (count % 2 != 0)
        {
            int index = (count + 1) / 2;
            int currentCount = 0;
            
            while (it.hasNext())
            {
                Map.Entry<Integer, Integer> entry = it.next();
                currentCount += entry.getValue();
                if (currentCount >= index)
                    return entry.getKey();
            }
        }
        else
        {
            int index1 = count / 2;
            int index2 = index1 + 1;
            int currentCount = 0;
            int median1 = 0;
            int median2 = 0;
            
            while (it.hasNext())
            {
                Map.Entry<Integer, Integer> entry = it.next();
                currentCount += entry.getValue();
                if (median1 == 0 && currentCount >= index1)
                    median1 = entry.getKey();
                if (median2 == 0 && currentCount >= index2)
                    median2 = entry.getKey();
                if (median1 != 0 && median2 != 0)
                    return (median1 + median2) / 2;
            }
        }
        
        return 0;
    }
    
    public static int setThreshold(int i)
    {
        return threshold = i <= 0 ? 500 : i;
    }
    
    public static boolean toggleKeepLoggingOnLag()
    {
        boolean b = keepLoggingOnLag;
        return keepLoggingOnLag = !b;
    }
    
    public static File dump()
    {
        if (watcherThread != null)
        {
            Object2IntMap.FastEntrySet<String> set;
            synchronized (watcherThread.lagSources)
            {
                set = watcherThread.lagSources.object2IntEntrySet();
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
        private final Object sync = new Object();
        private long tickStartMillis = 0L;
        private boolean newTick = false;
        
        private final Thread mainThread;
        private final Object2IntOpenHashMap<String> lagSources = new Object2IntOpenHashMap<>();
        
        public LagSpikeWatcherThread(Thread mainThread)
        {
            setName("LagSpikeWatcherThread");
            setDaemon(true);
            this.mainThread = mainThread;
        }
        
        @Override
        public void run()
        {
            tickStartMillis = System.currentTimeMillis();
            long lastTimeoutTime = 0L;
            long millis;
            boolean repeating;
            
            while (!exit)
            {
                synchronized (sync)
                {
                    millis = System.currentTimeMillis();
                    repeating = keepLoggingOnLag && millis - lastTimeoutTime >= threshold;
                    if (millis - tickStartMillis >= threshold && (newTick || repeating))
                    {
                        newTick = false;
                        lastTimeoutTime = millis;
                        timedOut(repeating);
                    }
                }
                
                Thread.yield();
                if (Thread.interrupted())
                {
                    exit = true;
                    break;
                }
            }
        }
        
        private void timedOut(boolean repeat)
        {
            StackTraceElement[] stack = mainThread.getStackTrace();
            StringBuilder stackStringBuilder = new StringBuilder();
            
            Tweakception.logger.debug("==================================================");
            if (!repeat)
                Tweakception.logger.debug("LagSpikeWatcher: tick lasted >" + threshold + "ms");
            else
                Tweakception.logger.debug("LagSpikeWatcher: tick lasted " + threshold + " more ms");
            
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
                
                stackStringBuilder.append(newline);
            }
            Tweakception.logger.debug("==================================================");
            
            String stackString = stackStringBuilder.toString();
            synchronized (lagSources)
            {
                lagSources.merge(stackString, 1, Integer::sum);
            }
            
            Tweakception.scheduler.add(() ->
            {
                if (McUtils.isInGame())
                {
                    McUtils.sendChat("LagSpikeWatcher: tick lasted >" + threshold + "ms");
                    McUtils.sendChat("stack top -> " + stack[0].getMethodName());
                }
            });
        }
        
        public void exit()
        {
            exit = true;
            interrupt();
        }
        
        public void newTick()
        {
            synchronized (sync)
            {
                tickStartMillis = System.currentTimeMillis();
                newTick = true;
            }
        }
    }
}
