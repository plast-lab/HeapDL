package heapdl.ctxenhancer.Agent;

import javassist.expr.*;
import javassist.*;

import java.io.*;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class Transformer implements ClassFileTransformer {

    private static boolean debug = true;
    private static String homeDir;
    private boolean optInstrumentCGE = true;
    public Transformer(boolean optInstrumentCGE) {
        this.optInstrumentCGE = optInstrumentCGE;
    }

    public static synchronized void premain(String args, Instrumentation inst) throws ClassNotFoundException, IOException, NotFoundException {
        this.homeDir = System.getenv("HOME");
        if (homeDir == null) {
            System.err.println("Cannot find HOME environment variable, aborting.");
            System.exit(-1);
        } else {
            debugMessage("Found HOME = " + homeDir);
        }

        ClassPool cp = ClassPool.getDefault();
        cp.insertClassPath(homeDir + "/doop-benchmarks/dacapo-bach/avrora.jar");
        cp.insertClassPath(homeDir + "/doop-benchmarks/dacapo-bach/avrora-deps.jar");
        boolean optCGE = (args != null) && args.contains("cg");
        inst.addTransformer(new Transformer(optCGE));

    }

    private static boolean isLibraryClass(String name) {
        return name == null || name.startsWith("java") || name.startsWith("Instrumentation") || name.startsWith("sun");
    }

    private static boolean isInterestingClass(String name) {
        String nameDots = name.replace("/", ".");
        if (nameDots.startsWith("javassist"))
            return false;
        if (nameDots.startsWith("Instrumentation"))
            return false;
        if (nameDots.startsWith("heapdl.ctxenhancer"))
            return false;
        if (nameDots.equals("java.lang.String"))
            return false;
        // if (nameDots.equals("java.lang.StringBuilder"))
        //     return false;
        debugMessage("interesting class: " + nameDots);
        return true;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain pd, byte[] classFile) throws IllegalClassFormatException {
        CtClass cls = null;
        if (isLibraryClass(className)) return null;
        debugMessage("Transforming: " + className);
        try {
            ClassPool cp = ClassPool.getDefault();
            cp.appendClassPath(homeDir + "/doop-benchmarks/dacapo-bach/avrora.jar");
            cp.appendClassPath(homeDir + "/doop-benchmarks/dacapo-bach/avrora-deps.jar");
            //cp.insertClassPath(new ByteArrayClassPath(className.replace("/","."), classFile));
            cls = cp.get(className.replace("/","."));

            //cls = getCtClass(className);
        } catch (Throwable e) {
            //e.printStackTrace();
            return null;
        }

        final CtClass finalCls = cls;

        Arrays.stream(cls.getDeclaredMethods()).forEach((CtMethod m) -> {
            try {
                if (Modifier.isNative(m.getModifiers()))
                    return;
                if (!Modifier.isStatic(m.getModifiers()) && !Modifier.isAbstract(m.getModifiers()) && optInstrumentCGE) {
                    debugMessage("Instrumentation#1");
                    m.insertBefore("heapdl.ctxenhancer.Recorder.Recorder.recordCall($0);");
                }

                m.instrument(new ExprEditor() {
                    public void edit(NewExpr newExpr) throws CannotCompileException {
                        if (!isInterestingClass(newExpr.getClassName()))
                           return;
                        if (Modifier.isStatic(m.getModifiers())) {
                            debugMessage("Instrumentation#2");
                            newExpr.replace("{ $_ = $proceed($$);   heapdl.ctxenhancer.Recorder.Recorder.recordStatic($_); }");
                        } else {
                            debugMessage("Instrumentation#3");
                            newExpr.replace("{ $_ = $proceed($$);   heapdl.ctxenhancer.Recorder.Recorder.record(this, $_); }");
                        }
                    }

                    public void edit(MethodCall call) throws CannotCompileException {
                        if (Modifier.isStatic(m.getModifiers())) {
                            debugMessage("Instrumentation#4");
                            call.replace(" { heapdl.ctxenhancer.Recorder.Recorder.mergeStatic(); $_ = $proceed($$); }");
                        } else {
                            debugMessage("Instrumentation#5");
                            call.replace(" { heapdl.ctxenhancer.Recorder.Recorder.merge(this); $_ = $proceed($$); }");
                        }
                    }

                    public void edit(NewArray newArray) throws CannotCompileException {
                        try {
                            if (!isInterestingClass(newArray.getComponentType().getName()))
                                return;
                        } catch (NotFoundException e) {
                            return;
                        }
                        if (Modifier.isStatic(m.getModifiers())) {
                            debugMessage("Instrumentation#6");
                            newArray.replace("{ $_ = $proceed($$);   heapdl.ctxenhancer.Recorder.Recorder.recordStatic($_); }");
                        } else {
                            debugMessage("Instrumentation#7");
                            newArray.replace("{ $_ = $proceed($$);   heapdl.ctxenhancer.Recorder.Recorder.record(this, $_); }");
                        }
                    }
                });


            } catch (Exception e) {
                // fail silently
                e.printStackTrace();
            }
        });
        try {
            cls.debugWriteFile("tmp");
            //System.out.println(cls.getName());
            return cls.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cls.detach();
        }
        return null;

    }

    private static void debugMessage(String msg) {
        if (debug)
            System.err.println("Context-Agent: " + msg);
    }

    private static CtClass getCtClass(String className) throws NotFoundException {
        ClassPool cp = ClassPool.getDefault();
        return cp.get(className.replace("/", "."));
    }
}
