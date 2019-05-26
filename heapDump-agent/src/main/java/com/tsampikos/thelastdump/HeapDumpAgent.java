package com.tsampikos.thelastdump;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


public class HeapDumpAgent {

    private static final String SYSTEM_CLASS = "java.lang.System";
    private static String heapDumpFilename = "heap-dump.hprof";
    private static boolean ignoreMain = false;

    public static void premain(final String agentArgs, final Instrumentation instrumentation) {
        actualMain(agentArgs, instrumentation);
    }

    public static void agentmain(final String agentArgs, final Instrumentation instrumentation) {
        actualMain(agentArgs, instrumentation);
    }

    private static void actualMain(final String agentArgs, final Instrumentation instrumentation) {
        processArguments(agentArgs);
        instrumentSystemClass(instrumentation);
        if (!ignoreMain) {
            instrumentMainClass(getJarMainClass(), instrumentation);
        }
    }

    private static void processArguments(String args) {
        if (args != null) {
            String[] arguments = args.split(",");
            for (String arg : arguments) {
                if (arg.equals("ignoreMain")) {
                    ignoreMain = true;
                } else if (arg.startsWith("file")) {
                    String filename = arg.split("=")[1];
                    if (!filename.endsWith(".hprof")) {
                        filename += ".hprof";
                    }
                    heapDumpFilename = filename;
                }
            }
        }
    }

    private static void instrumentSystemClass(Instrumentation instrumentation) {
        Class<?> targetCls;
        ClassLoader targetClassLoader;
        try {
            targetCls = Class.forName(SYSTEM_CLASS);
            targetClassLoader = targetCls.getClassLoader();

            SystemTransformer systemTransformer = new SystemTransformer(targetCls.getName(), targetClassLoader, heapDumpFilename);
            instrumentation.addTransformer(systemTransformer, true);
            try {
                instrumentation.retransformClasses(targetCls);
            } catch (Exception ex) {
                throw new RuntimeException("SystemTransform failed for class: [" + targetCls.getName() + "]", ex);
            }
        } catch (ClassNotFoundException ex) {
            System.out.println("CLASS NOT FOUND!");
        }
    }

    private static void instrumentMainClass(String jarMainClass, Instrumentation instrumentation) {
        Class<?> targetCls;
        ClassLoader targetClassLoader;
        try {
            targetCls = Class.forName(jarMainClass);
            targetClassLoader = targetCls.getClassLoader();

            MainTranformer mainTranformer = new MainTranformer(targetCls.getName(), targetClassLoader);
            instrumentation.addTransformer(mainTranformer, true);
            try {
                instrumentation.retransformClasses(targetCls);
            } catch (Exception ex) {
                throw new RuntimeException("MainTransform failed for class: [" + targetCls.getName() + "]", ex);
            }
        } catch (ClassNotFoundException ex) {
            System.out.println("CLASS NOT FOUND!");
        }
    }

    private static String getJarMainClass() {
        try {
            String jarFileName = System.getProperty("sun.java.command").split(" ")[0];
            File file = new File(jarFileName);
            if (file.exists()) {
                JarFile jarfile = new JarFile(jarFileName);
                Manifest manifest = jarfile.getManifest();
                String mainClass = manifest.getMainAttributes().getValue("Main-Class");
                if (mainClass != null) {
                    return mainClass;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}