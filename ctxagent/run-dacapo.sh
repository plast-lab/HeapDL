#!/bin/bash

JAVA=java

AGENT=${HOME}/doop-nexgen/HeapDL/ctxagent/build/libs/ctxagent-1.0-SNAPSHOT.jar

for b in avrora batik h2 luindex lusearch sunflow
# for b in avrora batik eclipse h2 jython luindex lusearch pmd sunflow tradebeans xalan
do
    time ${JAVA} -javaagent:${AGENT}=cg,${b} -Xss10000m -cp ${AGENT}:${HOME}/doop-benchmarks/dacapo-bach/dacapo-9.12-bach.jar Harness ${b} -s default -t 1
done
