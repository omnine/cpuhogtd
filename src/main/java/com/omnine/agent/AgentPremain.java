package com.omnine.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.jar.JarFile;
// this class is registered as the Premain-Class in the MANIFEST.MF of this jar
//
// this class should have minimal dependencies since it will live in the system class loader while
// the rest will live in the bootstrap class loader
public class AgentPremain {
    private static CPUMonitor cpuMonitor;

    private AgentPremain() {}

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("NanoAgent is running");


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
