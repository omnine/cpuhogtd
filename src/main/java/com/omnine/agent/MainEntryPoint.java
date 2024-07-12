package com.omnine.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class MainEntryPoint {
    private MainEntryPoint() {}

    private static CPUMonitor cpuMonitor;

    public static void premain(Instrumentation instrumentation, Class<?>[] allPriorLoadedClasses,
            File glowrootJarFile) {
        cpuMonitor = new CPUMonitor();
        cpuMonitor.startMonitoring();

        // Add a shutdown hook to stop the CPU monitoring when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (cpuMonitor != null) {
                cpuMonitor.stopMonitoring();
                System.out.println("CPU Monitoring stopped.");
            }
        }));

    }    
}
