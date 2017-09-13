# Context agent for HeapDL

To build the context agent:

```shell
gradle fatJar
```

To run test.jar with the context agent, assuming the CLASSPATH is the
current value of the class path needed for test.jar, run the following
command:

```shell
java -javaagent:build/libs/ctxagent-1.0-SNAPSHOT.jar=cg -cp build/libs/ctxagent-1.0-SNAPSHOT.jar:${CLASSPATH} test.jar
```

You can omit the `cg` parameter to avoid the performance hit of
instrumenting every method call to record its call-graph edge.
