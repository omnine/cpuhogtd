package com.omnine.agent;

import com.sun.management.OperatingSystemMXBean;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.Map;

import java.util.concurrent.TimeUnit;


public class CPUMonitor {
    private static final double CPU_THRESHOLD = 50.0;
    private static final long CHECK_INTERVAL_MS = 60000;	// 1 minute
    private static final long AGGRESSIVE_INTERVAL_MS = 10000;	// 10 seconds
    private static final double CONCERN_THRESHOLD = 12;	//	ABOUT last 2 minutes
    private final OperatingSystemMXBean osBean;
    private volatile boolean running = true; // Step 1: Volatile flag
    private boolean bAlarmOn = false;

    private double lastProcessCpuLoad = -1;
    
    int concern = 0;
    public CPUMonitor() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    }

    public void startMonitoring() {
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && running) {
                    double processCpuLoad = osBean.getProcessCpuLoad() * 100;

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
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

        try (FileWriter writer = new FileWriter("thread-dump-" + LocalDateTime.now() + ".txt")) {
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                StackTraceElement[] stackTraceElements = entry.getValue();

                writer.write("Thread: " + thread.getName() + " - State: " + thread.getState() + "\n");
                for (StackTraceElement element : stackTraceElements) {
                    writer.write("\tat " + element + "\n");
                }
                writer.write("\n");
            }
        }

        System.out.println("Thread dump captured.");
    }    
}
