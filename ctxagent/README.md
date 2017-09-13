# Context agent for HeapDL

To build the context agent, run the following command:

```shell
gradle fatJar
```

To run test.jar with the context agent, assuming `$CLASSPATH` is the
current value of the class path needed for test.jar, run the following
command:

```shell
java -javaagent:build/libs/ctxagent-1.0-SNAPSHOT.jar=cg -cp build/libs/ctxagent-1.0-SNAPSHOT.jar:$CLASSPATH test.jar
```

You can omit the `cg` agent parameter to avoid instrumenting every
method call to record its call-graph edge. This improves performance
but misses dynamic call graph edges.
