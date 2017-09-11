package heapdl.ctxenhancer.Agent;

import org.objectweb.asm.*;

/**
 * Almost like ASM's ClassWriter but with a custom class loader to
 * handle the loading of inner classes with custom loaders. For
 * example, the parent ClassWriter.getCommonSuperClass() method cannot
 * properly handle class "cck.util.Options" (and its nested class
 * "cck.util.Option$Str") in DaCapo benchmark "avrora".
 */
public class ContextClassWriter extends ClassWriter {
    private ClassLoader loader;

    public ContextClassWriter(ClassLoader l, ClassReader classReader, int flags) {
        super(classReader, flags);
        this.loader = l;
    }

    /**
     * Variant of org.objectweb.asm.ClassWriter.getCommonSuperClass()
     * that properly handles nested classes loaded by custom class
     * loaders (almost the same code).
     */
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        Class<?> c = null, d = null;
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            c = Class.forName(type1.replace('/', '.'), false, classLoader);
            d = Class.forName(type2.replace('/', '.'), false, classLoader);
        } catch (Exception e) {
            // If normal loading failed, try our own class loader.
            try {
                if (c == null)
                    c = Class.forName(type1.replace('/', '.'), false, loader);
                if (d == null)
                    d = Class.forName(type2.replace('/', '.'), false, loader);
            }
            catch (Exception e2) {
                System.out.println("class loader: " + classLoader +
                                   ", loader = " + loader);
                e2.printStackTrace();
                throw new RuntimeException(e2.toString());
            }
        }
        if (c.isAssignableFrom(d)) {
            return type1;
        } else if (d.isAssignableFrom(c)) {
            return type2;
        } else if (c.isInterface() || d.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                c = c.getSuperclass();
            } while (!c.isAssignableFrom(d));
            return c.getName().replace('.', '/');
        }
    }
}
