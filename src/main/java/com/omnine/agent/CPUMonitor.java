package com.omnine.agent;

import com.sun.management.OperatingSystemMXBean;


import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

public class CPUMonitor {
    private static final Logger logger = LoggerFactory.getLogger(CPUMonitor.class); 

    private double CPUThreshold = 60.0;
    private long CheckInterval = 60000;	// 1 minute
    private long AggressiveInterval = 1000;	// 1 second
    private int ConcernThreshold = 10;	//	ABOUT last 10 seconds
    private long LateStart = 300000;	// 5 minutes
    private double IgnoreUnder = 5.0;
    private boolean OutputCPU = false;
    private final OperatingSystemMXBean osBean;
    private volatile boolean running = true; // Step 1: Volatile flag
    private boolean bAlarmOn = false;

    private double lastProcessCpuLoad = -1;
    private long cpus = 0;

    Map<Long, Long> cpuTimes = new HashMap<>();
    Map<Long, Long> lastCPUTimes = new HashMap<>();
    Map<Long, Long> cpuTimeFetch = new HashMap<>();
    Map<Long, Long> lastCPUTimeFetch = new HashMap<>();

    ThreadMXBean threadMxBean;

    
    int concern = 0;

    private static final DecimalFormat decimalFormat = new DecimalFormat("#.###");
    private static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public CPUMonitor() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    public void loadConfig(String jarDir) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(jarDir + "/config.json")));
            JSONObject json = new JSONObject(content);

            if(json.has("CPUThreshold")) {
                CPUThreshold = json.getDouble("CPUThreshold");
            }

            if(json.has("CheckInterval")) {
                CheckInterval = json.getLong("CheckInterval");
            }

            if(json.has("AggressiveInterval")) {
                AggressiveInterval = json.getLong("AggressiveInterval");
            }

            if(json.has("ConcernThreshold")){
                ConcernThreshold = json.getInt("ConcernThreshold");
            }

            if(json.has("OutputCPU")){
                OutputCPU = json.getBoolean("OutputCPU");
            }

            if(json.has("LateStart")) {
                LateStart = json.getLong("LateStart");
                if(LateStart < 0)
                {
                    LateStart = 0;
                }
            }

            if (json.has("IgnoreUnder")) {
                IgnoreUnder = json.getDouble("IgnoreUnder");
                if(IgnoreUnder < 0.01)
                {
                    IgnoreUnder = 0.01;
                }
            }

        } catch (Exception e) {
//            e.printStackTrace();
            // Handle error or set default values
            logger.warn("Error loading config file. Use the default values.");
        }
    }

    public void startMonitoring() {
        threadMxBean = ManagementFactory.getThreadMXBean();
        // Check and enable CPU time measurement if supported
        if (threadMxBean.isThreadCpuTimeSupported()) {
            threadMxBean.setThreadCpuTimeEnabled(true);
        }
        else {
            logger.warn("Thread CPU time measurement is not supported.");
            return;
        }


        cpus = Runtime.getRuntime().availableProcessors();
        IgnoreUnder = IgnoreUnder / cpus;
        new Thread(() -> {          
            try {
                TimeUnit.MILLISECONDS.sleep(LateStart);

                while (!Thread.currentThread().isInterrupted() && running) {
                    double processCpuLoad = osBean.getProcessCpuLoad() * 100;
                    if(OutputCPU) {
                        logger.info("Current Process CPU Load: {}%", decimalFormat.format(processCpuLoad));
                    }
                    if (processCpuLoad >= CPUThreshold) {
                        if(!bAlarmOn) {
                            try {
                                recording(Thread.currentThread().getId());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        bAlarmOn = true;
                        concern++;
                        //this condition may be too strict
                        /*
                        if(processCpuLoad > lastProcessCpuLoad)
                        {
                            concern++;
                        }
                         */

                        
                        if(concern >= ConcernThreshold) {
                            try {
                                captureThreadDump(Thread.currentThread().getId());
                                concern = 0;    // reset
                                bAlarmOn = false;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        bAlarmOn = false;
                        concern = 0;
                    }
                    
                    lastProcessCpuLoad = processCpuLoad;
                    
                    if(bAlarmOn) {	// check it more aggressively
                        TimeUnit.MILLISECONDS.sleep(AggressiveInterval);
                    }
                    else {
                        TimeUnit.MILLISECONDS.sleep(CheckInterval);
                    }


                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                System.out.println("Monitoring thread was interrupted.");
            }
        }).start();
    }

    public void stopMonitoring() {
        running = false; // Step 3: Stop the monitoring

    }
    private void captureThreadDump(long idMonitorThread) throws IOException {
        ThreadInfo[] tis = threadMxBean.dumpAllThreads(false, false);

        for (int i = 0; i < tis.length; i++) 
        {
            // As we compare the CPU time between two check points, we should not skip the threads that are not runnable.
            /*
             if(tis[i].getThreadState() != Thread.State.RUNNABLE)
            {
                continue;
            }
             */



            long id = tis[i].getThreadId();
            if(id == idMonitorThread)
            {   // skip the monitoring thread
                continue;
            }

            Long idid = new Long(id);
            long current = threadMxBean.getThreadCpuTime(id);
            if(current < 0)
            {
                continue;
            }
            long now = System.currentTimeMillis();
            if (lastCPUTimes.get(idid) != null)
            {
                long prev = (Long) lastCPUTimes.get(idid);
                long catchTime = (Long) lastCPUTimeFetch.get(idid);

                System.out.println("current=" + current + " prev=" + prev); 
                double percent = (current - prev) / ((now - catchTime) * cpus * 10000.0);
                if (percent > IgnoreUnder)    // only check the increase
                {
                    logger.info("[{}%] \"{}\" #{}  cpu={}ms elapsed={}ms", decimalFormat.format(percent), tis[i].getThreadName(), id, (current-prev)/1000000, now - catchTime);
                    int depth = 1;
                    for (StackTraceElement ste : tis[i].getStackTrace()) {
                        String indent = repeat(" ", depth);  // Using 1 space for each level of indentation
                        logger.info("{}at {}", indent, ste);
                        depth++;
                    }
                }
            }
            cpuTimes.put(idid, current);  // new
            cpuTimeFetch.put(idid, now);
        }

        //This would not cause memory problem.
        lastCPUTimes.clear();
        lastCPUTimes.putAll(cpuTimes);
        cpuTimes.clear();

        lastCPUTimeFetch.clear();
        lastCPUTimeFetch.putAll(cpuTimeFetch);
        cpuTimeFetch.clear();


        System.out.println("Thread dump captured.");
    }    


    private void recording(long idMonitorThread) throws IOException {
        ThreadInfo[] tis = threadMxBean.dumpAllThreads(false, false);

        for (int i = 0; i < tis.length; i++) 
        {
            long id = tis[i].getThreadId();
            if(id == idMonitorThread)
            {   // skip the monitoring thread
                continue;
            }

            Long idid = new Long(id);
            long current = threadMxBean.getThreadCpuTime(id);
            if(current < 0)
            {
                continue;
            }
            long now = System.currentTimeMillis();
            lastCPUTimes.put(idid, current);  // new
            lastCPUTimeFetch.put(idid, now);
        }



        System.out.println("Start to record.");
    }    



}
