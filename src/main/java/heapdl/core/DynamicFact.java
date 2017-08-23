package heapdl.core;

import heapdl.io.Database;

/**
 * Created by neville on 28/01/2017.
 */
public interface DynamicFact {
    public void write_fact(Database db);

    default boolean isProbablyUnmatched() { return false; }
}
