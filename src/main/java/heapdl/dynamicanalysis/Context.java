package heapdl.dynamicanalysis;

import heapdl.io.Database;
import heapdl.io.PredicateFile;

/**
 * Created by neville on 15/03/2017.
 */
public interface Context extends DynamicFact {
    public static final String[] DEFAULT_CTX_ARGS = new String[] {"", "", "", "", "", ""};
    public static final String[] DEFAULT_CTXS = new String[] { ContextInsensitive.IMMUTABLE_DCTX };

    public static void write_facts_once(Database db) {
        for (String defaultCtx: DEFAULT_CTXS) {
            db.add(PredicateFile.DYNAMIC_CONTEXT, defaultCtx, DEFAULT_CTX_ARGS);
        }
    }

    String getRepresentation();
}
