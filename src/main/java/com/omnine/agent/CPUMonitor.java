package com.omnine.agent;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CPUMonitor {
    private final OperatingSystemMXBean osBean;
    private final ScheduledExecutorService scheduler;

    public CPUMonitor() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startMonitoring() {
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    double processCpuLoad = osBean.getProcessCpuLoad() * 100;
                    System.out.printf("Process CPU Load: %.2f%%%n", processCpuLoad);
                    
                    // Dynamic waiting period. This can be adjusted as needed.
                    long waitTimeMillis = 1000; // Example: wait for 1 second
                    Thread.sleep(waitTimeMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                System.out.println("Monitoring thread was interrupted.");
            }
        }).start();
    }

    public void stopMonitoring() {
        scheduler.shutdown();
    }
}
