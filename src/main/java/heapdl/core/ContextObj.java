package heapdl.core;

import heapdl.io.HeapDatabaseConsumer;
import heapdl.io.PredicateFile;

/**
 * Created by neville on 15/03/2017.
 */
public class ContextObj implements ComposableContext {

    private final int lineNumber;
    private final String inMethod;
    private final String type;
    private final String representation;
    private final boolean createHeapAllocation;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContextObj that = (ContextObj) o;

        return getRepresentation() != null ? getRepresentation().equals(that.getRepresentation()) : that.getRepresentation() == null;
    }

    @Override
    public int hashCode() {
        return getRepresentation() != null ? getRepresentation().hashCode() : 0;
    }

    public ContextObj(int lineNumber, String inMethod, String type, boolean createHeapAllocation) {
        this.lineNumber = lineNumber;
        this.inMethod = inMethod;
        this.type = type;
        this.createHeapAllocation = createHeapAllocation;
        this.representation = DynamicNormalHeapObject.getAllocationRepresentation(lineNumber, inMethod, type);
    }

    public ContextObj(String representation) {
        lineNumber = 0;
        type = null;
        inMethod = null;
        this.createHeapAllocation = false;
        this.representation = representation;
    }

    @Override
    public void write_fact(HeapDatabaseConsumer db) {
        String[] args = DEFAULT_CTX_ARGS.clone();
        args[0] = representation;
        db.add(PredicateFile.DYNAMIC_CONTEXT, representation, args);
        if (createHeapAllocation)
            db.add(PredicateFile.DYNAMIC_NORMAL_HEAP_ALLOCATION, ""+lineNumber, inMethod, type, representation);
    }

    public int getStartIndex() { return 0; }

    public String[] getComponents() {
        return new String[] { representation };
    }

    public String getRepresentation() {
        return representation;
    }


}
