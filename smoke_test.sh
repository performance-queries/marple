#! /bin/bash
shopt -s expand_aliases
. setup.sh
set -e
rm -rf *.class
antlr4 -visitor perf_query.g4
javac *.java
./test_everything.sh PerfQueryCompiler
./test_everything.sh Interpreter
