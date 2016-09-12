[![Build Status](https://travis-ci.org/anirudhSK/needlstk.svg?branch=master)](https://travis-ci.org/anirudhSK/needlstk)

needlstk: A system for expressing network performance queries

The name is a portmanteau of needle and stack to reflect the
goal of finding needles in a haystack. It is pronounced "needle stack"

QuickStart:

1. . setup.sh (Don't execute setup.sh, source it)
2. rm *.class rm PerfQuery*.java rm PerfQuery*.tokens
3. antlr4 -visitor PerfQuery.g4 && javac *.java
4. Generate parse tree using
grun PerfQuery prog  example_queries/flowlet_hist.sql -gui
(should open up a window showing the parse tree)
5. Run compiler using
cat example_queries/flowlet_hist.sql | java -ea Compiler 2> /tmp/tree.dot
6. Run interpreter using
cat example_queries/flowlet_hist.sql | java -ea Interpreter 2> /tmp/output.py
7. Generate expression tree using
dot -Tpng /tmp/tree.dot > /tmp/tree.png
8. Run interpreted code using
python3 /tmp/output.py

Some quick smoke tests (wrapped in ./smoke_test.sh)
grun perf_query prog example_queries/*.sql
./test_everything.sh Interpreter
./test_everything.sh PerfQueryCompiler

These tests don't mean too much, but if you get an exception for any one, it
probably means there's a bug.
