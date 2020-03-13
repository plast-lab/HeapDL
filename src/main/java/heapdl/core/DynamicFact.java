package heapdl.core;

import heapdl.io.HeapDatabaseConsumer;

/**
 * Created by neville on 28/01/2017.
 */
public interface DynamicFact {
    public void write_fact(HeapDatabaseConsumer db);

    default boolean isProbablyUnmatched() { return false; }
}
