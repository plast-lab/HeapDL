package heapdl.hprof;

abstract public class JavaThing {

    private final long id;
    private final long classId;
    private final String className;
    private final StackTrace stackTrace;

    public JavaThing(long id, long classId, String className, StackTrace stackTrace) {
        this.id = id;
        this.classId = classId;
        this.className = className;
        this.stackTrace = stackTrace;
    }

    public long getId() {
        return id;
    }

    public long getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public StackTrace getStackTrace() {
        return stackTrace;
    }

    public String toString() {
        return className + "@0x" + Long.toHexString(classId);
    }
}
