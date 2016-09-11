#! /bin/bash
shopt -s expand_aliases
. setup.sh;
set -e;
grun perf_query prog example_queries/*.sql
./test_everything.sh PerfQueryCompiler
./test_everything.sh Interpreter
