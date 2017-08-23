package heapdl.main;

import java.util.Map;

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
                    + "  -h --help     Show this screen.\n"
                    + "  --version     Show version.\n"
                    + "  --sensitivity=<sensitivity>  Context sensitivity (e.g. 2ObjH) [default: Insensitive].\n"
                    + "  --unique-strings      Whether or not to extract string constants from heap dump.\n"
                    + "\n";
    public static void main(String[] args) {
            Map<String, Object> opts =
                    new Docopt(doc).withVersion("HeapDL 1.0").parse(args);
            MemoryAnalyser m = new MemoryAnalyser((String)opts.get("<hprof1>"), (boolean) opts.get("--unique-strings"));
    }
}
