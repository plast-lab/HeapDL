package heapdl.core;

import heapdl.hprof.StackFrame;
import heapdl.hprof.StackTrace;
import heapdl.io.Database;
import heapdl.io.PredicateFile;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by neville on 22/02/2017.
 */
public class DynamicCallGraphEdge implements DynamicFact {

    private final String contextFrom;
    private final String contextTo;
    private final String methodFrom;
    private final int lineNumberFrom;

    private final String methodTo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DynamicCallGraphEdge that = (DynamicCallGraphEdge) o;

        if (!methodFrom.equals(that.methodFrom)) return false;
        if (lineNumberFrom != that.lineNumberFrom) return false;
        if (!contextFrom.equals(that.contextFrom)) return false;
        if (!contextTo.equals(that.contextTo)) return false;

        return methodTo.equals(that.methodTo);
    }

    @Override
    public int hashCode() {

        int result = methodFrom.hashCode();
        result = 31 * result + lineNumberFrom;
        result = 31 * result + methodTo.hashCode();
        result = 31 * result + contextFrom.hashCode();
        result = 31 * result + contextTo.hashCode();
        return result;
    }

    public DynamicCallGraphEdge(String methodFrom, String lineNumberFrom, String methodTo, String contextFrom, String contextTo) {
        this.contextFrom = contextFrom;
        this.contextTo = contextTo;
        this.methodFrom = methodFrom;
        this.lineNumberFrom = DumpParsingUtil.parseLineNumber(lineNumberFrom);
        this.methodTo = methodTo;
    }

    public static Collection<DynamicCallGraphEdge> fromStackTrace(StackTrace trace) {
        if (trace == null || trace.getFrames() == null)
            return new ArrayList<>();
        StackFrame[] frames = trace.getFrames();
        ArrayList<DynamicCallGraphEdge> edges = new ArrayList<>(frames.length - 1);
        for (int i = 1 ; i < frames.length; i ++) {
            if (frames[i].getClassName().startsWith("heapdl"))
                return edges;
        }
        for (int i = 1 ; i < frames.length; i ++) {
            edges.add(new DynamicCallGraphEdge(
                    DumpParsingUtil.fullyQualifiedMethodSignatureFromFrame(frames[i]),
                    frames[i].getLineNumber(),
                    DumpParsingUtil.fullyQualifiedMethodSignatureFromFrame(frames[i-1]),
                    ContextInsensitive.get().getRepresentation(), ContextInsensitive.get().getRepresentation())
            );
        }

        return edges;
    }

    @Override
    public String toString() {
        return "DynamicCallGraphEdge{" +
                "methodFrom='" + methodFrom + '\'' +
                ", lineNumberFrom='" + lineNumberFrom + '\'' +
                ", methodTo='" + methodTo + '\'' +
                '}';
    }

    @Override
    public void write_fact(Database db) {
        db.add(PredicateFile.DYNAMIC_CALL_GRAPH_EDGE, methodFrom, "" + lineNumberFrom, methodTo, contextFrom, contextTo);
    }
}
