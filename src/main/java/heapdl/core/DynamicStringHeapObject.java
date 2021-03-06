package heapdl.core;

import heapdl.io.HeapDatabaseConsumer;
import heapdl.io.FactEncoders;

import static heapdl.io.PredicateFile.*;

/**
 * Created by neville on 15/02/2017.
 */
public class DynamicStringHeapObject implements DynamicHeapObject {
    private final String representation;

    public DynamicStringHeapObject(String stringValue) {
        stringValue = stringValue.replace("\n", "").replace("\t", "").replace("\r", "");
        this.representation = FactEncoders.encodeStringConstant(stringValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DynamicStringHeapObject that = (DynamicStringHeapObject) o;

        return getRepresentation().equals(that.getRepresentation());
    }

    @Override
    public int hashCode() {
        return getRepresentation().hashCode();
    }

    public String getRepresentation() {
        return representation;
    }

    @Override
    public String getContextRepresentation() {
        return ContextInsensitive.get().getRepresentation();
    }

    @Override
    public String getHeapRepresentation() {
        return representation;
    }

    @Override
    public boolean isProbablyUnmatched() {
        return false;
    }

    @Override
    public void write_fact(HeapDatabaseConsumer db) {
        db.add(DYNAMIC_NORMAL_HEAP_ALLOCATION,"0", "", "java.lang.String", representation);
        db.add(DYNAMIC_NORMAL_HEAP_OBJECT,representation, ContextInsensitive.get().getRepresentation(), representation);
        db.add(STRING_RAW, representation, representation);
        db.add(STRING_CONST, representation);
    }
}
