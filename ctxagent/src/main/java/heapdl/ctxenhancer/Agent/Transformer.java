package heapdl.ctxenhancer.Agent;

import java.io.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Arrays;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

public class Transformer implements ClassFileTransformer {

    private static boolean debug = false;
    private boolean optInstrumentCGE = true;
    public Transformer(boolean optInstrumentCGE, String benchmark) {
        this.optInstrumentCGE = optInstrumentCGE;
    }

    public static synchronized void premain(String args, Instrumentation inst) throws ClassNotFoundException, IOException {

        boolean optCGE = (args != null) && args.contains("cg");

        final String[] benchmarks = new String[] { "avrora", "batik", "eclipse", "h2", "jython", "luindex", "lusearch", "pmd", "sunflow", "tradebeans", "xalan" };
        String benchmark = null;
        for (String b : benchmarks) {
            if (args != null && args.contains(b)) {
                benchmark = b;
                break;
            }
        }
        if (benchmark == null) {
            System.err.println("No suitable benchmark defined in agent options: " + args);
            System.exit(-1);
        } else {
            inst.addTransformer(new Transformer(optCGE, benchmark));
        }
    }

    private static boolean isLibraryClass(String name) {
        return name == null || name.startsWith("java") || name.startsWith("Instrumentation") || name.startsWith("sun") || name.startsWith("heapdl");
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain pd, byte[] classFile) throws IllegalClassFormatException {
        if (isLibraryClass(className)) return null;
        debugMessage("Transforming: " + className);

        ClassReader reader = new ClassReader(classFile);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS |
                                                     ClassWriter.COMPUTE_FRAMES);
        ClassVisitor ctxAdapter = new CtxEnhancherAdapter(writer, className, optInstrumentCGE);
        reader.accept(ctxAdapter, ClassReader.EXPAND_FRAMES);

        byte[] ret = writer.toByteArray();
        if (debug) {
            // Save bytecode.
            debugWriteClass(className, ret);
            debugWriteClass(className + ".orig", classFile);

            // Check bytecode using ASM's CheckClassAdapter.
            ClassReader debugReader = new ClassReader(ret);
            ClassWriter debugWriter = new ClassWriter(reader,
                                                      ClassWriter.COMPUTE_MAXS |
                                                      ClassWriter.COMPUTE_FRAMES);
            try {
                debugReader.accept(new CheckClassAdapter(debugWriter), 0);
            } catch (RuntimeException ex) {
                System.err.println("Bytecode check failed for " + className + ":");
                ex.printStackTrace();
                System.exit(-1);
            }

        }

        return ret;
    }

    private void debugWriteClass(String className, byte[] bytecode) {
        try {
            String outDir = "out";
            (new java.io.File(outDir)).mkdir();
            OutputStream out = new FileOutputStream(outDir + "/" + className.replace("/", "_") + ".class");
            out.write(bytecode);
            out.flush();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /*
    // Original implementation using Javassist.
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain pd, byte[] classFile) throws IllegalClassFormatException {
        CtClass cls = null;
        if (isLibraryClass(className)) return null;
        debugMessage("Transforming: " + className);
        try {
            ClassPool cp = ClassPool.getDefault();
            cp.appendClassPath(homeDir + dacapoJar);
            cp.appendClassPath(homeDir + dacapoDeps);
            //cp.insertClassPath(new ByteArrayClassPath(className.replace("/","."), classFile));
            cls = cp.get(className.replace("/","."));

            //cls = getCtClass(className);
        } catch (Throwable e) {
            e.printStackTrace();
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
    */

    public static void debugMessage(String msg) {
        if (debug) {
            System.err.println("Context-Agent: " + msg);
            System.err.flush();
        }
    }
}
