needlstk: A system for expressing network performance queries

The name is a portmanteau of needle and stack to reflect the
goal of finding needles in a haystack. It is pronounced "needle stack"

QuickStart:

1. . setup.sh (Don't execute setup.sh, source it)
2. antlr4 Sql.g4 
3. javac Sql*.java
4. grun Sql prog -gui
GROUPBY [srcip, dstip], def foo ( [a, b], [srcip, dstip]) : a = 1 [srcip] AS [srcip] T
^D

(should open up a window showing the parse tree)
