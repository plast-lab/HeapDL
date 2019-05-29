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
            codeBlock.append("try {");
            codeBlock.append("Process p = Runtime.getRuntime().exec(\"jmap -dump:format=b,file=" + this.heapDumpFilename + " \" + pid);");
            codeBlock.append("p.waitFor();");
            codeBlock.append("} catch (Exception e) {");
            codeBlock.append("System.err.println(\"Could not execute jmap! Please make sure it's in your PATH.\");");
            codeBlock.append("}");
            m.insertBefore(codeBlock.toString());

            byteCode = cc.toBytecode();
            cc.detach();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
        }
        return byteCode;
    }
}
