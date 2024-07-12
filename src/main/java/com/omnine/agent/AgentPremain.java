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
    private static final boolean PRE_CHECK_LOADED_CLASSES =
    Boolean.getBoolean("glowroot.debug.preCheckLoadedClasses");

    private AgentPremain() {}

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("MyAgent is running");
        try {
            Class<?>[] allPriorLoadedClasses;
            if (PRE_CHECK_LOADED_CLASSES) {
                allPriorLoadedClasses = instrumentation.getAllLoadedClasses();
            } else {
                allPriorLoadedClasses = new Class<?>[0];
            }
            CodeSource codeSource = AgentPremain.class.getProtectionDomain().getCodeSource();
            // suppress warnings is used instead of annotating this method with @Nullable
            // just to avoid dependencies on other classes (in this case the @Nullable annotation)
            @SuppressWarnings("argument.type.incompatible")
            File glowrootJarFile = new File("C:/temp/cpuhog/cpuhogtd-1.0-SNAPSHOT.jar");
            Class<?> mainEntryPointClass;
            if (glowrootJarFile != null) {
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(glowrootJarFile));
            }
            mainEntryPointClass = Class.forName("org.glowroot.agent.MainEntryPoint", true,
                    AgentPremain.class.getClassLoader());
            Method premainMethod = mainEntryPointClass.getMethod("premain", Instrumentation.class,
                    Class[].class, File.class);
            premainMethod.invoke(null, instrumentation, allPriorLoadedClasses, glowrootJarFile);
        } catch (Throwable t) {
            // log error but don't re-throw which would prevent monitored app from starting
            System.err.println("Glowroot failed to start: " + t.getMessage());
            t.printStackTrace();
        }





    }
}
