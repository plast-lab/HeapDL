package heapdl.ctxenhancer.Agent;

import java.io.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Arrays;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

public class Transformer implements ClassFileTransformer {

    // Debugging flags.
    private final static boolean debug = false;
    private final static boolean saveBytecode = true;

    private final String outDir = "out";

    private boolean optInstrumentCGE = true;
    public Transformer(boolean optInstrumentCGE, String benchmark) {
        this.optInstrumentCGE = optInstrumentCGE;
        if (debug)
            System.err.println("Context-Agent: debugging is on");
        if (saveBytecode)
            System.err.println("Context-Agent: transformed bytecode is saved under '" + outDir + "'");
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
        debugMessage("Transforming: " + className + " [loader=" + loader + ']');

        ClassReader reader = new ClassReader(classFile);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS |
                                                     ClassWriter.COMPUTE_FRAMES);
        ClassVisitor ctxAdapter = new CtxEnhancherAdapter(writer, className, optInstrumentCGE, loader);
        reader.accept(ctxAdapter, ClassReader.EXPAND_FRAMES);

        byte[] ret = writer.toByteArray();
        if (debug || saveBytecode) {
            // Save bytecode.
            debugWriteClass(className, ret);
            debugWriteClass(className + ".orig", classFile);
        }
        if (debug) {
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

    public static void debugMessage(String msg) {
        if (debug) {
            System.err.println("Context-Agent: " + msg);
            System.err.flush();
        }
    }
}
