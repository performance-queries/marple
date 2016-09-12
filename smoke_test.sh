#! /bin/bash
shopt -s expand_aliases
. setup.sh
set -e
rm -rf *.class PerfQuery*.java PerfQuery*.tokens
antlr4 -visitor PerfQuery.g4
javac *.java
./test_everything.sh Compiler
./test_everything.sh Interpreter
