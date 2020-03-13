package heapdl.core;

import heapdl.hprof.StackFrame;
import heapdl.hprof.StackTrace;
import heapdl.io.HeapDatabaseConsumer;
import heapdl.io.PredicateFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by neville on 16/02/2017.
 */
public class DynamicReachableMethod implements DynamicFact {
    private final String representation;

    public static Collection<DynamicReachableMethod> fromStackTrace(StackTrace trace) {
        if (trace == null || trace.getFrames() == null)
            return new ArrayList<>();
        StackFrame[] frames = trace.getFrames();
        for (int i = 1 ; i < frames.length; i ++) {
            if (frames[i].getClassName().startsWith("heapdl"))
                return new ArrayList<>();
        }
        return Arrays.stream(frames).map(
                a -> new DynamicReachableMethod(DumpParsingUtil.fullyQualifiedMethodSignatureFromFrame(a))
        ).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "DynamicReachableMethod{" +
                "representation='" + representation + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DynamicReachableMethod that = (DynamicReachableMethod) o;

        return representation != null ? representation.equals(that.representation) : that.representation == null;
    }

    @Override
    public int hashCode() {
        return representation != null ? representation.hashCode() : 0;
    }

    public DynamicReachableMethod(String representation) {
        this.representation = representation;
    }

    @Override
    public void write_fact(HeapDatabaseConsumer db) {
        db.add(PredicateFile.DYNAMIC_REACHABLE_METHOD, representation);

    }
}
