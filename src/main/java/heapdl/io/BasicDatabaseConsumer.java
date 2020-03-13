package heapdl.io;

import java.io.IOException;

/**
 * A simple implementation of a heap database consumer.
 */
public class BasicDatabaseConsumer implements HeapDatabaseConsumer {
    private final Database db;

    public BasicDatabaseConsumer(Database db) {
        this.db = db;
    }

    @Override
    public void add(PredicateFile predicateFile, String arg, String... args) {
        db.add(predicateFile, arg, args);
    }

    @Override
    public void finishWriting() throws IOException {
        db.flush();
        db.close();
    }
}
