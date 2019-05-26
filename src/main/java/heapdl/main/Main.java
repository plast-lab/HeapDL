package heapdl.main;

import java.io.File;
import java.util.Map;
import java.util.List;

import heapdl.core.MemoryAnalyser;
import org.docopt.Docopt;

/**
 * Created by neville on 23/08/2017.
 */
public class Main {

    private static final String defaultSensitivity = "Insensitive";

    private static final String doc =
            "Heaps Don't Lie!\n"
                    + "\n"
                    + "Usage:\n"
                    + "  heapdl <file> ... [--stackTraces=<traces>...] --out=<dir> [--sensitivity=<sensitivity>] [--no-strings]\n"
                    + "  heapdl (-h | --help)\n"
                    + "  heapdl --version\n"
                    + "\n"
                    + "Options:\n"
                    + "  -h --help                    Show this screen.\n"
                    + "  --version                    Show version.\n"
                    + "  --stackTraces=<traces>...    Add stack traces.\n"
                    + "  --sensitivity=<sensitivity>  Context sensitivity (2ObjH or Insensitive) [default: " + defaultSensitivity + "].\n"
                    + "  --no-strings                 Do not extract short string constants from heap dump.\n"
                    + "  --out=<dir>                  Output directory.\n"
                    + "\n";

    public static void main(String[] args) {
        String version = Main.class.getPackage().getImplementationVersion();
        Map<String, Object> opts =
                new Docopt(doc)
                        .withVersion("HeapDL "+ (version == null ? "DEVELOPMENT" : version))
                        .parse(args);

        // Read command-line options.
        List<String> hprofs = (List<String>)opts.get("<file>");
        List<String> stackTraces = (List<String>)opts.get("--stackTraces");
        String sensitivity = (String)opts.get("--sensitivity");
        if (sensitivity == null) {
            sensitivity = defaultSensitivity;
        }
        System.out.println("Using sensitivity: " + sensitivity);
        String factsDir = (String)opts.get("--out");
        if (factsDir == null) {
            System.err.println("Missing --out parameter for output directory.");
            return;
        }

        File factsDirFile = new File(factsDir);
        if (!factsDirFile.exists()) {
            System.out.println("Creating output directory: " + factsDir);
            factsDirFile.mkdirs();
        } else {
            System.out.println("Using existing output directory: " + factsDir);
        }

        // Generate facts from the HPROF inputs.
        MemoryAnalyser m = new MemoryAnalyser(hprofs, stackTraces, !((boolean) opts.get("--no-strings")));
        try {
            int n = m.getAndOutputFactsToDB(new File(factsDir), sensitivity);
            System.out.println("Generated " + n + " facts.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
