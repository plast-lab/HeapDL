# Heap dump agent
Takes a heap dump on program exit.
Currently supports two ways of exiting. The first is by using the System.exit(). The second is by exiting normally through the main function (only for JARs where the Main-Class attribute is defined).

To build the heap dump agent, run the following command:

```shell
mvn clean install
```

To run Program.jar with the heap dumpe agent, run the following command:

```shell
java -javaagent:target/theLastDump-1.0-SNAPSHOT-jar-with-dependencies.jar -jar Program.jar
```

## Optional agent arguments
**ignoreMain**: Skips the main instrumentation.

**file=\<file\>**: Write dump to specific file. The default value is _heap-dump.hprof_.
