# Stack traces agent
Works only with JDK11+ due to [JEP 331](https://openjdk.java.net/jeps/331) and [JEP 318](https://openjdk.java.net/jeps/318).

To build the stack traces agent, run the following command:

```shell
make
```

To run Program.jar with the stack traces agent, run the following command:

```shell
java -agentpath:./libStackTracesAgent.so -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -jar Program.jar
```

## Optional agent arguments
**file=\<file\>**: Write stack traces to specific file. The default value is _stackTraces.csv_.

**depth=\<size\>**: Stack trace depth. The default value is 8.

**sampling=\<size\>**: Sampling interval. eg. 128kb, 1MB, 1024. The default value is 512kb.
