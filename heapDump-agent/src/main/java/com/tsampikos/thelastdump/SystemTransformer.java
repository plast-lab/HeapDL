package com.tsampikos.thelastdump;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class SystemTransformer implements ClassFileTransformer {

    private String targetClassName;
    private ClassLoader targetClassLoader;
    private String heapDumpFilename;

    private static final String METHOD = "exit";

    public SystemTransformer(String targetClassName, ClassLoader targetClassLoader, String heapDumpFilename) {
        this.targetClassName = targetClassName;
        this.targetClassLoader = targetClassLoader;
        this.heapDumpFilename = heapDumpFilename;
    }

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain protectionDomain, byte[] bytes) {
        byte[] byteCode = bytes;

        String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/"); //replace . with /
        if (!name.equals(finalTargetClassName)) {
            return byteCode;
        }
        try {
            ClassPool cp = ClassPool.getDefault();

            CtClass cc = cp.get(targetClassName);

            CtMethod m = cc.getDeclaredMethod(METHOD);
            m.addLocalVariable("pid", CtClass.intType);

            StringBuilder codeBlock = new StringBuilder();

            codeBlock.append("pid = ProcessHandleImpl.current().pid();");
            codeBlock.append("Process p = Runtime.getRuntime().exec(\"jmap -dump:format=b,file=" + this.heapDumpFilename + " \" + pid);");
            codeBlock.append("p.waitFor();");
            m.insertBefore(codeBlock.toString());

            byteCode = cc.toBytecode();
            cc.detach();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
            System.err.println("Could not execute jmap, please check whether it is in your PATH.");
        }
        return byteCode;
    }
}
