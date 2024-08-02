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

    private double CPU_THRESHOLD = 50.0;
    private long CHECK_INTERVAL_MS = 60000;	// 1 minute
    private long AGGRESSIVE_INTERVAL_MS = 5000;	// 5 seconds, this should not be too big than 5 seconds
    private double CONCERN_THRESHOLD = 6;	//	ABOUT last 30 seconds
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

    public CPUMonitor() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    public void loadConfig(String jarDir) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(jarDir + "/config.json")));
            JSONObject json = new JSONObject(content);

            CPU_THRESHOLD = json.getDouble("CPU_THRESHOLD");
            CHECK_INTERVAL_MS = json.getLong("CHECK_INTERVAL_MS");
            AGGRESSIVE_INTERVAL_MS = json.getLong("AGGRESSIVE_INTERVAL_MS");
            CONCERN_THRESHOLD = json.getDouble("CONCERN_THRESHOLD");
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
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && running) {
                    double processCpuLoad = osBean.getProcessCpuLoad() * 100;
                    logger.info("Current Process CPU Load: {}%", decimalFormat.format(processCpuLoad));

                    if (processCpuLoad >= CPU_THRESHOLD) {
                        bAlarmOn = true;
                        concern++;
                        //this condition may be too strict
                        /*
                        if(processCpuLoad > lastProcessCpuLoad)
                        {
                            concern++;
                        }
                         */

                        
                        if(concern >= CONCERN_THRESHOLD) {
                            try {
                                captureThreadDump(Thread.currentThread().getId());
                                concern = 0;    // reset
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
                        TimeUnit.MILLISECONDS.sleep(AGGRESSIVE_INTERVAL_MS);
                    }
                    else {
                        TimeUnit.MILLISECONDS.sleep(CHECK_INTERVAL_MS);
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
                if (percent > 0)    // only check the increase
                {
                    logger.info("[{}%] \"{}\" #{}  cpu={}ms elapsed={}ms", decimalFormat.format(percent), tis[i].getThreadName(), id, (current-prev)/1000000, now - catchTime);
                    for (StackTraceElement ste : tis[i].getStackTrace()) {
                        logger.info("at {}", ste);
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
}
