package heapdl.core;

import heapdl.hprof.StackFrame;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by neville on 27/01/2017.
 */
public class DumpParsingUtil {
    public static final int UNKNOWN_LINE = 0;
    private static final Map<String, String> cachedRaisedTypes = new ConcurrentHashMap<>();

    private static final String BOOLEAN = "boolean";
    private static final String BOOLEAN_JVM = "Z";
    private static final String INT = "int";
    private static final String INT_JVM = "I";
    private static final String LONG = "long";
    private static final String LONG_JVM = "J";
    private static final String DOUBLE = "double";
    private static final String DOUBLE_JVM = "D";
    private static final String VOID = "void";
    private static final String VOID_JVM = "V";
    private static final String FLOAT = "float";
    private static final String FLOAT_JVM = "F";
    private static final String CHAR = "char";
    private static final String CHAR_JVM = "C";
    private static final String SHORT = "short";
    private static final String SHORT_JVM = "S";
    private static final String BYTE = "byte";
    private static final String BYTE_JVM = "B";

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
//                System.out.println(compact);
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
        return frame.getMethodSignature().replace("<MethodName>", frame.getMethodName());
    }


    public static String raiseTypeId(String id) {
        String cached = cachedRaisedTypes.get(id);
        if (cached != null)
            return cached;

        int typePrefixEndIdx = 0;
        // Peel off array brackets.
        while (id.charAt(typePrefixEndIdx) == '[')
            typePrefixEndIdx++;

        StringBuilder sb;
        if ((id.charAt(typePrefixEndIdx) == 'L') && (id.charAt(id.length() -1) == ';'))
            sb = new StringBuilder(id.substring(typePrefixEndIdx + 1, id.length() - 1).replace('/', '.'));
        else
            sb = new StringBuilder(decodePrimType(id.substring(typePrefixEndIdx)));

        if (typePrefixEndIdx != 0) {
            for (int i = 0; i < typePrefixEndIdx; i++)
                sb.append("[]");
        }

        String ret = sb.toString();

        // Find multidimensional arrays in bytecode (e.g. '[[C' / 'char[][]') .
//        if (typePrefixEndIdx > 1) {
//            System.err.println("Warning: found multidimensional array type: " + id + " -> " + ret);
//        }

        cachedRaisedTypes.put(id, ret);
        return ret;
    }
    private static String decodePrimType(String id) {
        switch (id) {
            case BOOLEAN_JVM : return BOOLEAN;
            case INT_JVM     : return INT;
            case LONG_JVM    : return LONG;
            case DOUBLE_JVM  : return DOUBLE;
            case VOID_JVM    : return VOID;
            case FLOAT_JVM   : return FLOAT;
            case CHAR_JVM    : return CHAR;
            case SHORT_JVM   : return SHORT;
            case BYTE_JVM    : return BYTE;
            default          : throw new RuntimeException("Invalid type id format: " + id);
        }
    }

    public static String convertArguments(String arguments) {
    	//System.out.println("Unraised arguments: " + arguments);
    	String raisedArguments = "";
	    String arrayPostfix = "";
    	int i = 0;
    	while (i < arguments.length()) {
		    if (arguments.charAt(i) == 'L') {
			    String lowLevelType = arguments.substring(i, arguments.indexOf(';', i) + 1);
			    //System.out.println("Low level type: " + lowLevelType);
			    if (arguments.indexOf(';', i) != arguments.length() - 1) {
				    raisedArguments += raiseTypeId(lowLevelType) + arrayPostfix + ",";
			    }
			    else {
				    raisedArguments += raiseTypeId(lowLevelType) + arrayPostfix;
			    }
			    arrayPostfix = "";
			    i = arguments.indexOf(';', i) + 1;
			}
		    else if (arguments.charAt(i) == '[') {
		    	arrayPostfix += "[]";
		    	i++;
		    }
		    else {
		    	String lowLevelPrimType = "" + arguments.charAt(i);
		    	if (i != arguments.length() -1) {
				    raisedArguments += raiseTypeId(lowLevelPrimType) + arrayPostfix + ",";
			    }
		    	else {
				    raisedArguments += raiseTypeId(lowLevelPrimType) + arrayPostfix;
			    }
		    	i++;
		    }
	    }
    	//System.out.println("Raised arguments: " + raisedArguments);
    	return raisedArguments;
    }

	public static boolean isPrimitiveType(String s) {
		return (s.equals(BOOLEAN) || s.equals(INT) || s.equals(LONG) ||
				s.equals(DOUBLE) || s.equals(VOID) || s.equals(FLOAT) ||
				s.equals(CHAR) || s.equals(SHORT) || s.equals(BYTE));
	}

	public static boolean isLowLevelType(char first, String s) {
		return first == '[' || (first == 'L' && s.endsWith(";"));
	}
}
