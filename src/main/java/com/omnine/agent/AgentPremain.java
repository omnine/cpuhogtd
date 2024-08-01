package com.omnine.agent;

import java.io.File;

import java.lang.instrument.Instrumentation;

import java.security.CodeSource;

// this class is registered as the Premain-Class in the MANIFEST.MF of this jar
//
// this class should have minimal dependencies since it will live in the system class loader while
// the rest will live in the bootstrap class loader
public class AgentPremain {
    private static CPUMonitor cpuMonitor;

    private AgentPremain() {}

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("NanoAgent is running");

        try {
            CodeSource codeSource = CPUMonitor.class.getProtectionDomain().getCodeSource();
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            String jarDir = jarFile.getParentFile().getPath();
//            System.err.println("JAR directory: " + jarDir);
            System.setProperty("agent.jar.dir", jarDir);

            cpuMonitor = new CPUMonitor();
            cpuMonitor.loadConfig(jarDir);
            cpuMonitor.startMonitoring();
    
            // Add a shutdown hook to stop the CPU monitoring when the application exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (cpuMonitor != null) {
                    cpuMonitor.stopMonitoring();
                    System.out.println("CPU Monitoring stopped.");
                }
            }));


        } catch (Exception e) {
            System.err.println("Failed to determine JAR directory");
        }        






    }
}
