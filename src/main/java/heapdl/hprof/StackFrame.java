package heapdl.hprof;


public class StackFrame {
    private static final int LINE_NUMBER_UNKNOWN = -1;
    private static final int LINE_NUMBER_COMPILED = -2;
    private static final int LINE_NUMBER_NATIVE = -3;
    private final String methodName;
    private final String methodSignature;
    private final String className;
    private final int lineNumber;

    public StackFrame(String methodName, String methodSignature, String className, int lineNumber) {
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.className = className;
        this.lineNumber = lineNumber;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getMethodSignature() {
        return this.methodSignature;
    }

    public String getClassName() {
        return this.className;
    }

    public String getLineNumber() {
        switch (this.lineNumber) {
            case LINE_NUMBER_NATIVE:
                return "(native method)";
            case LINE_NUMBER_COMPILED:
                return "(compiled method)";
            case LINE_NUMBER_UNKNOWN:
                return "(unknown)";
            default:
                return Integer.toString(this.lineNumber, 10);
        }
    }
}
