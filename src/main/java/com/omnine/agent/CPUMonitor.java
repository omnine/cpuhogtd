package com.omnine.agent;

import com.sun.management.OperatingSystemMXBean;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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
    private long AGGRESSIVE_INTERVAL_MS = 10000;	// 10 seconds
    private double CONCERN_THRESHOLD = 12;	//	ABOUT last 2 minutes
    private final OperatingSystemMXBean osBean;
    private volatile boolean running = true; // Step 1: Volatile flag
    private boolean bAlarmOn = false;

    private double lastProcessCpuLoad = -1;

    static Map<Long, Long> cpuTimes = new HashMap<>();
    static Map<Long, Long> lastCPUTimes = new HashMap<>();
    static Map<Long, Long> cpuTimeFetch = new HashMap<>();
    static Map<Long, Long> lastCPUTimeFetch = new HashMap<>();

    
    int concern = 0;
    public CPUMonitor() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        loadConfig();

    }

    private void loadConfig() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("config.json")));
            JSONObject json = new JSONObject(content);

            CPU_THRESHOLD = json.getDouble("CPU_THRESHOLD");
            CHECK_INTERVAL_MS = json.getLong("CHECK_INTERVAL_MS");
            AGGRESSIVE_INTERVAL_MS = json.getLong("AGGRESSIVE_INTERVAL_MS");
            CONCERN_THRESHOLD = json.getDouble("CONCERN_THRESHOLD");
        } catch (Exception e) {
//            e.printStackTrace();
            // Handle error or set default values
            logger.warn("Error loading config file. Using default values.");
        }
    }

    public void startMonitoring() {
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && running) {
                    double processCpuLoad = osBean.getProcessCpuLoad() * 100;
                    logger.info("Current Process CPU Load: {}%", processCpuLoad);

                    if (processCpuLoad >= CPU_THRESHOLD) {
                        bAlarmOn = true;
                        if(processCpuLoad > lastProcessCpuLoad)
                        {
                            concern++;
                        }
                        
                        if(concern >= CONCERN_THRESHOLD) {
                            try {
                                captureThreadDump();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        bAlarmOn = false;
                        concern = 0;
                    }
                    
                    lastProcessCpuLoad = processCpuLoad;
                    
                    if(bAlarmOn) {	// check it more agressively
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
    private static void captureThreadDump() throws IOException {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        // Check and enable CPU time measurement if supported
        if (threadMxBean.isThreadCpuTimeSupported()) {
            threadMxBean.setThreadCpuTimeEnabled(true);
        }
        else {
            logger.warn("Thread CPU time measurement is not supported.");
        }

        long cpus = Runtime.getRuntime().availableProcessors();



        ThreadInfo[] t = threadMxBean.dumpAllThreads(false, false);

        for (int i = 0; i < t.length; i++) 
        {
            if(t[i].getThreadState() != Thread.State.RUNNABLE)
            {
                continue;
            }


            long id = t[i].getThreadId();
            Long idid = new Long(id);
            long current = threadMxBean.getThreadCpuTime(id);
            if(current == -1)
            {
                continue;
            }
            long now = System.currentTimeMillis();
            if (lastCPUTimes.get(idid) != null)
            {

                long prev = (Long) lastCPUTimes.get(idid);
                current = threadMxBean.getThreadCpuTime(t[i].getThreadId());
                long catchTime = (Long) lastCPUTimeFetch.get(idid);
                double percent = (current - prev) / ((now - catchTime) * cpus * 10000);
                if (percent > 0 && prev > 0)
                {
                    logger.info("Thread: {} | CPU Usage: {}% | CPU Time: {}%", t[i].getThreadName(), percent, current);
                    for (StackTraceElement ste : t[i].getStackTrace()) {
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
