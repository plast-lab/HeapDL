package heapdl.io;

import java.io.IOException;

/**
 * This interface can be implemented by clients using HeapDL as a
 * library.
 * @see BasicDatabaseConsumer
 */
public interface HeapDatabaseConsumer {
    /**
     * Record a tuple in a table.
     *
     * @param table   the table
     * @param arg     the value of the first column
     * @param args    the values of the rest of the columns
     */
    void add(PredicateFile table, String arg, String... args);

    /**
     * Finish writing to the database.
     *
     * @throws IOException if the operation failed
     */
    void finishWriting() throws IOException;
}
