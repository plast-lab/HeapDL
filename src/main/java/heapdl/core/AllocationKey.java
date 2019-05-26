package heapdl.core;

import heapdl.hprof.StackFrame;

/**
 * Created by neville on 23/05/2017.
 */
public class AllocationKey {


    private int hash = 0;
    private StackFrame thisFrame;
    private long classId;

    public int hashCode() {
        return this.hash;
    }

    public AllocationKey(StackFrame frame, long classId) {
        this.thisFrame = frame;
        this.classId = classId;
        if (frame == null) return;
        this.hash = frame.getClassName().hashCode();
        this.hash *= 31;
        this.hash += frame.getMethodName().hashCode();
        this.hash *= 31;
        this.hash += frame.getMethodSignature().hashCode();
        this.hash *= 31;
        this.hash += frame.getLineNumber().hashCode();
        this.hash *= 31;
        this.hash += classId;
    }

    public boolean equals(Object that) {
        if (!(that instanceof AllocationKey))
            return false;
        StackFrame thatFrame = ((AllocationKey) that).thisFrame;
        if (thisFrame == thatFrame)
            return true;
        return thatFrame.getClassName().equals(thisFrame.getClassName()) &&
                thatFrame.getMethodName().equals(thisFrame.getMethodName()) &&
                thatFrame.getLineNumber().equals(thisFrame.getLineNumber()) &&
                thatFrame.getMethodSignature().equals(thisFrame.getMethodSignature()) &&
                this.classId == ((AllocationKey) that).classId;
    }

    public StackFrame getFrame() {
        return thisFrame;
    }


}
