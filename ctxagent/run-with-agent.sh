#!/bin/bash

JAVA=java

AGENT=${HOME}/doop-nexgen/HeapDL/ctxagent/build/libs/ctxagent-1.0-SNAPSHOT.jar
CG=cg
# CG=

time ${JAVA} -javaagent:${AGENT}=${CG},external -Xss10000m -cp ${AGENT}:. ${1}
