package com.omnine.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;
// this class is registered as the Premain-Class in the MANIFEST.MF of this jar
//
// this class should have minimal dependencies since it will live in the system class loader while
// the rest will live in the bootstrap class loader
public class AgentPremain {

    private AgentPremain() {}
    private static CPUMonitor cpuMonitor;

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("MyAgent is running");

        File jarFile = new File("/path/to/yourjarfile.jar");
        try {
            inst.appendToBootstrapClassLoaderSearch(new JarFile(jarFile));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
