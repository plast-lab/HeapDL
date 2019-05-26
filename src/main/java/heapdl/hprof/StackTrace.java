package heapdl.hprof;


public class StackTrace {
    private final StackFrame[] frames;

    public StackTrace(StackFrame[] frames) {
        this.frames = frames;
    }

    public StackFrame[] getFrames() {
        return this.frames;
    }
}
