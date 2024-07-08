package com.omnine.agent;

import java.lang.instrument.Instrumentation;

public class NanoAgent {
    private static CPUMonitor cpuMonitor;

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("MyAgent is running");
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
