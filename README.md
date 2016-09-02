needlstk: A system for expressing network performance queries

The name is a portmanteau of needle and stack to reflect the
goal of finding needles in a haystack. It is pronounced "needle stack"

QuickStart:

1. . setup.sh (Don't execute setup.sh, source it)
2. antlr4 perf_query.g4 
3. javac *.java
4. grun Sql prog -gui
i = GROUPBY [srcip, dstip], def foo ( [a, b], [srcip, dstip]) : a = 1 [srcip] AS [srcip] T
^D
5. Run typechecker using
cat example_queries/flowlet_hist.sql | java -ea PerfQueryCompiler

(should open up a window showing the parse tree)
