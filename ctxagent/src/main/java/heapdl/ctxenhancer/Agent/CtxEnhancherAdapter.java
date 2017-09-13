package heapdl.ctxenhancer.Agent;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

import static heapdl.ctxenhancer.Agent.Transformer.*;

public class CtxEnhancherAdapter extends ClassVisitor {
    private String className;
    private boolean optInstrumentCGE;
    private ClassLoader loader;

    private static Map<ClassLoader, Set<String>> seenClasses = new ConcurrentHashMap<>();

    // Dummy class loader object representing the "null" system class loader.
    private static ClassLoader nullLoader;
    static {
        try {
            nullLoader = new URLClassLoader(new URL[] { new URL("http://dummy") });
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public CtxEnhancherAdapter(ClassWriter cw, String className, boolean optInstrumentCGE, ClassLoader loader) {
        super(Opcodes.ASM5, cw);
        this.className = className;
        this.optInstrumentCGE = optInstrumentCGE;
        this.loader = loader;
    }

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {
        MethodVisitor defaultVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (defaultVisitor == null)
            stopWithError("NULL default method visitor");

        // Native methods are ignored.
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            debugMessage("Ignoring native method: " + name + " in " + className);
            return defaultVisitor;
        }
        if (!canTransformClass(className, loader)) {
            debugMessage("Ignoring class " + className);
            return defaultVisitor;
        }
        debugMessage("Visiting method: " + className + "." + name + desc);

        // Non-static, non-abstract methods can be subject to
        // call-graph edge instrumentation.
        boolean isStatic   = (access & Opcodes.ACC_STATIC  ) != 0;
        boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
        boolean instrCGE   = (!isStatic) && (!isAbstract) && optInstrumentCGE;

        return new MethodEntryAdapter(access, name, desc, defaultVisitor, className, instrCGE, isStatic, loader);
    }

    private static boolean canTransformClass(String name, ClassLoader loader) {
        synchronized (seenClasses) {
            String nameDots = name.replace("/", ".");

            // Use a dummy object for the bootstrap loader.
            if (loader == null)
                loader = nullLoader;
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
        private static final boolean DONT_MERGE_FOR_INIT = true;

        private String className, methName, desc;
        private boolean instrCGE;
        private boolean isStatic;
        private boolean isInit;
        private ClassLoader loader;

        // Computes the extra stack requirements for the
        // instrumentation in this method. Currently unused (TODO).
        private int extraStack;

        // Used in the two-step instrumentation of NEW.
        private Stack<String> lastNewTypes;

        // Instruction index (first, second, ...). Not bytecode index.
        private int instrNum = -1;

        public MethodEntryAdapter(int access,
                                  String methName,
                                  String desc,
                                  MethodVisitor mv,
                                  String className,
                                  boolean instrCGE,
                                  boolean isStatic,
                                  ClassLoader loader) {
            super(Opcodes.ASM5, mv, access, methName, desc);
            this.className    = className;
            this.methName     = methName;
            this.desc         = desc;
            this.instrCGE     = instrCGE;
            this.isStatic     = isStatic;
            this.extraStack   = 0;
            this.lastNewTypes = new Stack<>();
            this.loader       = loader;
            this.isInit       = methName.equals("<init>");
        }

        @Override
        protected void onMethodEnter() {
            // If CGE is enabled, start method with recordCall().
            if (instrCGE) {
                if (DONT_MERGE_FOR_INIT && isInit) {
                    debugMessage("Ignoring recordCall() for " + getMethName());
                } else {
                    debugMessage("Insert recordCall() invocation in " + className + ":" + methName + ":" + methodDesc + "...");

                    // Call heapdl.ctxenhancer.Recorder.Recorder.recordCall(),
                    // using "this" as its argument.
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                          "heapdl/ctxenhancer/Recorder/Recorder",
                                          "recordCall", "(Ljava/lang/Object;)V", false);
                }
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
                                      "(Ljava/lang/Object;)V", false);
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
                                      "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
            }
        }

        private void callMerge() {
            debugMessage("merging in " + (isStatic ? "static" : "instance") +
                         " method " + methName);
            if (isInit) {
                // TODO: we should be able to merge() inside
                // constructors too, but we have to do it after the
                // this/super.<init>().
                // stopWithError("Cannot merge inside constructors yet.");
                debugMessage("Cannot merge inside constructors yet.");
                return;
            }
            if (isStatic) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                      "heapdl/ctxenhancer/Recorder/Recorder",
                                      "mergeStatic",
                                      "()V", false);
            } else {
                extraStack += 1;
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                      "heapdl/ctxenhancer/Recorder/Recorder",
                                      "merge",
                                      "(Ljava/lang/Object;)V", false);
            }
        }

        @Override
        public void visitEnd() {
            debugMessage("End of " + getMethName() +
                         ", instrNum = " + instrNum);
            super.visitEnd();
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            instrNum++;
            super.visitFieldInsn(opcode, owner, name, desc);
        }
        @Override
        public void visitIincInsn(int var, int increment) {
            instrNum++;
            super.visitIincInsn(var, increment);
        }
        @Override
        public void visitInsn(int opcode) {
            instrNum++;
            super.visitInsn(opcode);
        }
        @Override
        public void visitIntInsn(int opcode, int operand) {
            instrNum++;
            super.visitIntInsn(opcode, operand);
        }
        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            instrNum++;
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
        @Override
        public void visitJumpInsn(int opcode, Label label) {
            instrNum++;
            super.visitJumpInsn(opcode, label);
        }
        @Override
        public void visitLdcInsn(Object cst) {
            instrNum++;
            super.visitLdcInsn(cst);
        }
        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            instrNum++;
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }
        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            instrNum++;
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }
        @Override
        public void visitVarInsn(int opcode, int var) {
            instrNum++;
            super.visitVarInsn(opcode, var);
        }

        private String getMethName() {
            return className.replace("/", ".") + "." + methName + desc;
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

            if (name == null)
                stopWithError("null name in visitMethodInsn()");

            instrNum++;
            boolean callsInit = name.equals("<init>");

            // Instrument non-constructor invocations to call merge().
            if (!callsInit && DONT_MERGE_FOR_INIT) {
                debugMessage("Ignoring merge() for " + getMethName());
                callMerge();
            }

            // Generate the original invocation instruction.
            super.visitMethodInsn(opcode, owner, name, desc, itf);

            // Instrument constructor invocations to call record()
            // after 'new T' objects have been initialized.
            if (callsInit) {
                if (isInit) {
                    // TODO: don't ignore allocations inside constructors.
                } else if (lastNewTypes.empty()) {
                    // Sanity check: for instrumentation to work, we must have
                    // already seen a NEW instruction (unless we are already
                    // inside another <init>, in which case this can be a call
                    // to "super.<init>()" or another constructor of the
                    // current class).
                    debugMessage("No 'new " + owner + "()' found before constructor call: " + owner + "() in " + getMethName());
                } else {
                    // Record new objects after their <init> is called.
                    recordNewObjAfterInitCall(owner);
                }
            }
        }

        void recordNewObjAfterInitCall(String newType) {
            if (!canTransformClass(newType, loader))
                return;

            debugMessage("Recording " + newType + " object @ instr#" + instrNum);

            // Pop stack to show that one NEW was handled.
            String lastNewType = lastNewTypes.pop();
            debugMessage("Popping type: " + lastNewType +
                         ", peek = " +
                         (lastNewTypes.empty()? "NOTHING" : lastNewTypes.peek()));

            // Sanity check: if the types don't match then our
            // heuristic is buggy and can corrupt code.
            if (!lastNewType.equals(newType))
                stopWithError("Heuristic failed: lastNewType = " + lastNewType +
                              ", newType = " + newType +
                              ", method = " + getMethName() +
                              ", instr#" + instrNum);

            debugMessage("Instrumenting NEW/<init> for type " + newType + " in method " + methName);

            // We assume that the NEW already did a DUP (JVM spec
            // 4.10.2.4): since invokespecial(<init>) consumes the
            // extra value, there is still one left for us to use.
            recordNewObj();
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            instrNum++;
            super.visitTypeInsn(opcode, type);
            if (opcode == Opcodes.NEW) {
                // Java's 'new T()' becomes a series of instructions
                // 'new T; ...; T.<init>()' (JVM spec 4.10.2.4). We
                // cannot put instrumentation after NEW, as it is
                // illegal to access the reference it created until
                // <init> runs. Thus, when 'NEW T' is seen, we push T
                // to a stack. T will be popped when the constructor
                // body is visited; then, we can access "this" and
                // call instrumentation.
                debugMessage("Pushing new type: " + type +
                             ", empty? " + lastNewTypes.empty());
                if (canTransformClass(type, loader))
                    lastNewTypes.push(type);
            } else if (opcode == Opcodes.NEWARRAY) {
                if (canTransformClass(type, loader)) {
                    debugMessage("Instrumenting NEWARRAY " + type + " in method " + methName + ":" + desc);
                    recordNewObj();
                }
            }
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            instrNum++;
            super.visitMultiANewArrayInsn(desc, dims);
            if (canTransformClass(desc, loader)) {
                debugMessage("Instrumenting ANEWARRAY " + desc + "/" + dims + " in method " + methName + ":" + desc);
                recordNewObj();
            }
        }
    }
}
