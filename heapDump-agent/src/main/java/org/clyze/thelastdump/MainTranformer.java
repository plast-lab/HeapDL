package com.tsampikos.thelastdump;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class MainTranformer implements ClassFileTransformer {

    private String targetClassName;
    private ClassLoader targetClassLoader;

    private static final String METHOD = "main";

    public MainTranformer(String targetClassName, ClassLoader targetClassLoader) {
        this.targetClassName = targetClassName;
        this.targetClassLoader = targetClassLoader;
    }

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain protectionDomain, byte[] bytes) {
        byte[] byteCode = bytes;

        String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/");
        if (!name.equals(finalTargetClassName)) {
            return byteCode;
        }

        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(targetClassName);

            CtMethod m = cc.getDeclaredMethod(METHOD);

            StringBuilder codeBlock = new StringBuilder();
            codeBlock.append("System.exit(0);");

            m.insertAfter(codeBlock.toString());

            byteCode = cc.toBytecode();
            cc.detach();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
        }
        return byteCode;
    }
}
