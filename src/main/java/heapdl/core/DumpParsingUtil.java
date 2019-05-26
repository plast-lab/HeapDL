package heapdl.core;

import heapdl.hprof.StackFrame;

import java.util.Arrays;

/**
 * Created by neville on 27/01/2017.
 */
public class DumpParsingUtil {
    public static final int UNKNOWN_LINE = 0;

    public static int parseLineNumber(String lineNumber) {
        try {
            int a = Integer.parseInt(lineNumber);
            return a;
        } catch (NumberFormatException e) {
            return UNKNOWN_LINE;
        }
    }

    // Java compact type representation
    // Z                        boolean
    // B                        byte
    // C                        char
    // S                        short
    // I                        int
    // J                        long
    // F                        float
    // D                        double
    // L fully-qualified-class; fully-qualified-class
    // [ type                   type[]
    // ( arg-types ) ret-type   method type


    public static String[] convertType(String compact) {
        if (compact.length() == 0) return new String[]{"", ""};
        String first = compact.substring(0, 1);
        StringBuilder res = null;
        if ("ZBCSIJFDV".contains(first)) {
            switch (first) {
                case "Z":
                    res = new StringBuilder("boolean");
                    break;
                case "B":
                    res = new StringBuilder("byte");
                    break;
                case "C":
                    res = new StringBuilder("char");
                    break;
                case "S":
                    res = new StringBuilder("short");
                    break;
                case "I":
                    res = new StringBuilder("int");
                    break;
                case "J":
                    res = new StringBuilder("long");
                    break;
                case "F":
                    res = new StringBuilder("float");
                    break;
                case "D":
                    res = new StringBuilder("double");
                    break;
                case "V":
                    res = new StringBuilder("void");
                    break;
            }
            return new String[]{res.toString(), compact.substring(1)};
        } else {

            if (compact.startsWith("L")) {
                String[] temp = compact.substring(1).split("\\;");
                if (temp.length == 0)
                    throw new RuntimeException("Truncated input?");
                res = new StringBuilder(temp[0].replace('/', '.'));
                String rest = String.join(";", Arrays.copyOfRange(temp, 1, temp.length));
                return new String[]{res.toString(), rest};
            } else if (compact.startsWith("[")) {
                String[] temp = convertType(compact.substring(1));
                return new String[]{temp[0] + "[]", temp[1]};
            } else if (compact.startsWith("(")) {
                String[] temp = compact.substring(1).split("\\)");
                if (temp.length != 2)
                    throw new RuntimeException("Unknown: " + compact);
                res = new StringBuilder(convertType(temp[1])[0] + " <MethodName>(");
                String compactArgTypes = temp[0];
                do {
                    temp = convertType(compactArgTypes);
                    compactArgTypes = temp[1];
                    res.append(temp[0]).append(",");
                } while (compactArgTypes.length() > 0);
                return new String[]{res.substring(0, res.length() - 1) + ")", ""};

            } else throw new RuntimeException("Unknown: " + compact);
        }
    }

//    static Snapshot getSnapshotFromFile(String filename) {

//        File temp = new File(filename);
//        Snapshot model = null;


//        System.out.println("Reading from " + temp.getAbsolutePath() + "...");
//        try {
//            model = com.sun.tools.hat.internal.parser.Reader.readFile(temp.getAbsolutePath(), true, 0);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            System.exit(1);
//        } catch (RuntimeException ex) {
//            ex.printStackTrace();
//            System.exit(1);
//        }
//        System.out.println("Snapshot read, resolving...");
//        model.resolve(true);
//        System.out.println("Snapshot resolved.");


//        return model;
//    }


    public static String fullyQualifiedMethodSignatureFromFrame(StackFrame frame) {
        return "<" + frame.getClassName() + ": " + methodSignatureFromStackFrame(frame) + ">";
    }

    public static String methodSignatureFromStackFrame(StackFrame frame) {
        return convertType(frame.getMethodSignature())[0].replace("<MethodName>", frame.getMethodName());
    }

}
