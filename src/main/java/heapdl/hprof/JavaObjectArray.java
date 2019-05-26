package heapdl.hprof;

public class JavaObjectArray extends JavaThing {
    private final long[] elements;

    public JavaObjectArray(long id, long classId, String className, long[] elements, StackTrace stackTrace) {
        super(id, classId, className, stackTrace);
        this.elements = elements;
    }

    public long[] getElements() {
        return elements;
    }
}
