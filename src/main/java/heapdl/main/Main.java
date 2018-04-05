package heapdl.main;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import heapdl.core.MemoryAnalyser;
import org.docopt.Docopt;

/**
 * Created by neville on 23/08/2017.
 */
public class Main {

    private static final String doc =
            "Heaps Don't Lie!\n"
                    + "\n"
                    + "Usage:\n"
                    + "  heapdl <hprof1> [<hprof2>] [<hprof3>]\n"
                    + "  heapdl (-h | --help)\n"
                    + "  heapdl --version\n"
                    + "\n"
                    + "Options:\n"
                    + "  -h --help                    Show this screen.\n"
                    + "  --version                    Show version.\n"
                    + "  --sensitivity=<sensitivity>  Context sensitivity (e.g. 2ObjH) [default: Insensitive].\n"
                    + "  --no-strings                 Do not extract short string constants from heap dump.\n"
                    + "\n";

    public static void main(String[] args) {
            String version = Main.class.getPackage().getImplementationVersion();
            Map<String, Object> opts =
                    new Docopt(doc)
                            .withVersion("HeapDL "+ (version == null ? "DEVELOPMENT" : version))
                            .parse(args);

            List<String> hprofs = new ArrayList<String>();
            hprofs.add((String)opts.get("<hprof1>"));
            MemoryAnalyser m = new MemoryAnalyser(hprofs, !((boolean) opts.get("--no-strings")));
    }
}
