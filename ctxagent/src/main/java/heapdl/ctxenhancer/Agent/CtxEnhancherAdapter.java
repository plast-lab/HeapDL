package heapdl.ctxenhancer.Agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static heapdl.ctxenhancer.Agent.Transformer.debugMessage;

public class CtxEnhancherAdapter extends ClassVisitor {
    private String className;
    private boolean optInstrumentCGE;
    private ClassLoader loader;

    public CtxEnhancherAdapter(ClassWriter cw, String className, boolean optInstrumentCGE, ClassLoader loader) {
        super(Opcodes.ASM5, cw);
        this.className = className;
        this.optInstrumentCGE = optInstrumentCGE;
        this.loader = loader;
    }

    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {
        MethodVisitor defaultVisitor = super.visitMethod(access, name, desc, signature, exceptions);

        // Native methods are ignored.
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            debugMessage("Ignoring native method: " + name + " in " + className);
            return defaultVisitor;
        }
        if (!canTransformClass(className, loader)) {
            debugMessage("Ignoring class " + className);
            return defaultVisitor;
        }

        // Non-static, non-abstract methods can be subject to
        // call-graph edge instrumentation.
        boolean isStatic   = (access & Opcodes.ACC_STATIC  ) != 0;
        boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
        boolean instrCGE   = (!isStatic) && (!isAbstract) && optInstrumentCGE;

        return new MethodEntryAdapter(access, name, desc, defaultVisitor, className, instrCGE, isStatic);
    }

    private static Map<ClassLoader, Set<String>> seenClasses = new ConcurrentHashMap<>();
    private static boolean canTransformClass(String name, ClassLoader loader) {
        synchronized (seenClasses) {
            String nameDots = name.replace("/", ".");

            // Get the loaded classes for this loader and check that the
            // class has not already been transformed.
            Set<String> classesForLoader = seenClasses.get(loader);
            if (classesForLoader == null) {
                classesForLoader = new HashSet<>();
                seenClasses.put(loader, classesForLoader);
            } else if (classesForLoader.contains(name)) {
                debugMessage("Class has already been transformed, is no longer interesting: " + name);
                return false;
            }

            if (nameDots.startsWith("javassist"))
                return false;
            if (nameDots.startsWith("Instrumentation"))
                return false;
            if (nameDots.startsWith("heapdl"))
                return false;
            if (nameDots.equals("java.lang.Integer"))
                return false;
            if (nameDots.equals("java.lang.String"))
                return false;
            // if (nameDots.equals("java.lang.StringBuilder"))
            //     return false;
            debugMessage("interesting class: " + nameDots + " [loader=" + loader + ']');
            classesForLoader.add(nameDots);
            return true;
        }
    }

    static class MethodEntryAdapter extends AdviceAdapter {
        private String className, methName, desc;
        private boolean instrCGE;
        private boolean isStatic;

        // Computes the extra stack requirements for the
        // instrumentation in this method. Currently unused (TODO).
        private int extraStack;

        // Used in the two-step instrumentation of new().
        private String lastNewType = null;

        public MethodEntryAdapter(int access,
                                  String methName,
                                  String desc,
                                  MethodVisitor mv,
                                  String className,
                                  boolean instrCGE,
                                  boolean isStatic) {
            super(Opcodes.ASM5, mv, access, methName, desc);
            this.className = className;
            this.methName  = methName;
            this.desc      = desc;
            this.instrCGE  = instrCGE;
            this.isStatic  = isStatic;
            this.extraStack = 0;
        }

        @Override
        protected void onMethodEnter() {
            if (instrCGE) {
                debugMessage("Insert recordCall() invocation in " + className + ":" + methName + ":" + methodDesc + "...");

                // Call heapdl.ctxenhancer.Recorder.Recorder.recordCall(),
                // using "this" as its argument.
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                      "heapdl/ctxenhancer/Recorder/Recorder",
                                      "recordCall", "(Ljava/lang/Object;)V");
            }
        }

        // Records the creation of new objects without changing the
        // stack size needed for the current method.
        private void recordNewObj() {
            if (isStatic) {
                extraStack += 1;
                super.visitInsn(Opcodes.DUP);
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                      "heapdl/ctxenhancer/Recorder/Recorder",
                                      "recordStatic",
                                      "(Ljava/lang/Object;)V");
            } else {
                extraStack += 2;
                super.visitInsn(Opcodes.DUP);
                // Leaving out the following NOP creates a wrong D2I.
                super.visitInsn(Opcodes.NOP);
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitInsn(Opcodes.SWAP);
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                      "heapdl/ctxenhancer/Recorder/Recorder",
                                      "record",
                                      "(Ljava/lang/Object;Ljava/lang/Object;)V");
            }
        }

        private void callMerge() {
            if (isStatic) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                      "heapdl/ctxenhancer/Recorder/Recorder",
                                      "mergeStatic",
                                      "()V");
            } else {
                extraStack += 1;
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                      "heapdl/ctxenhancer/Recorder/Recorder",
                                      "merge",
                                      "(Ljava/lang/Object;)V");
            }
        }

        @Override
        public void visitEnd() {
            debugMessage("End of " + methName);
            super.visitEnd();
        }

        @Override
        public void visitMethodInsn(int opcode,
                                    String owner,
                                    String name,
                                    String desc,
                                    boolean itf) {

            // Ignore existing calls to the context agent.
            if (owner.startsWith("heapdl/ctxenhancer")) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            // TODO: currently we don't instrument constructor bodies.
            if (methName.equals("<init>")) {
                debugMessage("Ignoring call to " + owner + ":" + name + "() in body of " + className + "." + methName + "()");
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            // Call "merge" before calling non-<init> methods. For
            // <init> methods, look further down in this method.
            if (!name.equals("<init>")) {
                callMerge();
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);

            // If a call to <init> is found and a NEW instruction has
            // not been consumed yet, and the types match, then
            // instrument the initialized object.
            if (name != null && name.equals("<init>") && lastNewType != null) {

                // If the types don't match then our heuristic is
                // buggy and can corrupt code.
                if (!lastNewType.equals(owner)) {
                    System.err.println("Heuristic failed: lastNewType = " + lastNewType + ", owner = " + owner);
                }

                // Reset lastNewType.
                lastNewType = null;

                debugMessage("Instrumenting NEW/<init> for type " + owner + " in method " + methName + ":" + desc);

                // We assume that the NEW already did a DUP: since the
                // invokespecial(<init>) consumes the duplicate value,
                // there is still one left for us to use.
                recordNewObj();
            }

            // When calling <init> methods, we cannot call "merge"
            // before their call, as that will cause the bytecode
            // verifier to fail. We thus merge after the call, losing
            // only those calls to <init> that do not return (due to
            // effects such as exceptions or exiting the program).
            if (name.equals("<init>")) {
                callMerge();
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            super.visitTypeInsn(opcode, type);
            if (opcode == Opcodes.NEW) {
                lastNewType = type;
            } else if (opcode == Opcodes.NEWARRAY) {
                debugMessage("Instrumenting NEWARRAY " + type + " in method " + methName + ":" + desc);
                recordNewObj();
            }
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            super.visitMultiANewArrayInsn(desc, dims);
            debugMessage("Instrumenting ANEWARRAY " + desc + "/" + dims + " in method " + methName + ":" + desc);
            recordNewObj();
        }
    }
}
