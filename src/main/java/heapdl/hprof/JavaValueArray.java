package heapdl.hprof;

public class JavaValueArray extends JavaThing {

    private final Byte[] elements;
    private int length = 0;

    public JavaValueArray(long id, long classId, String className, Byte[] elements, StackTrace stackTrace) {
        super(id, classId, className, stackTrace);
        this.elements = elements;
        if (elements != null) {
            this.length = elements.length;
        }
    }

    public Byte[] getElements() {
        return elements;
    }

    public int getLength() {
        return length;
    }
}
