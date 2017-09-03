package heapdl.core;

import com.sun.tools.hat.internal.model.JavaClass;
import heapdl.io.Database;
import heapdl.io.FactEncoders;

import static heapdl.io.PredicateFile.*;

/**
 * Created by neville on 15/02/2017.
 */
public class DynamicClassHeapObject implements DynamicHeapObject {
    private final String representation;

    public DynamicClassHeapObject(JavaClass className) {
        this.representation = FactEncoders.encodeClass(className);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DynamicClassHeapObject that = (DynamicClassHeapObject) o;

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
    public void write_fact(Database db) {
        db.add(DYNAMIC_NORMAL_HEAP_ALLOCATION,"", "", "java.lang.Class", representation);
        db.add(DYNAMIC_NORMAL_HEAP_OBJECT,representation, ContextInsensitive.get().getRepresentation(), representation);
    }
}
