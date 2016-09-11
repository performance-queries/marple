grammar perf_query;

// Hide whitespace, but don't skip it
WS : [ \n\t\r]+ -> channel(HIDDEN);

// Keywords
SELECT : 'SELECT' | 'select' ;
WHERE : 'WHERE' | 'where' ;
FROM : 'FROM' | 'from' ;
GROUPBY : 'GROUPBY' | 'groupby';
JOIN   : 'JOIN' | 'join';
AS     : 'AS' | 'as';
IF     : 'IF' | 'if';
THEN   : 'THEN' | 'then';
ELSE   : 'ELSE' | 'else';
DEF    : 'def';
EMIT   : 'emit()';
TRUE   : 'true'  | 'TRUE';
FALSE  : 'false' | 'FALSE';
AND    : 'and';
OR     : 'or';
INFINITY : 'INFINITY' | 'infinity';

// Identifiers
ID : ('a'..'z' | 'A'..'Z' |  '_') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*;
VALUE : [0-9]+;

// alias ID to stream, column, agg_func and relations
// reuses the parser for type checking as well
state    : ID;
stream   : ID;
agg_func : ID;
column   : ID;

// Column list
column_with_comma : ',' column;
column_list : '[' column ']'
            | '*'
	    | '[' ']'
            | '[' column column_with_comma+ ']';

// List of state variables
state_with_comma : ',' state;
state_list : '[' state ']'
           | '*'
           | '[' ']'
           | '[' state state_with_comma+ ']';

// Expressions
expr : ID       # exprCol
     | VALUE    # exprVal
     | INFINITY # exprInf
     | expr op=('+'|'-'|'*'|'/') expr # exprComb
     | '(' expr ')' # exprParen
     ;

// Expression list
expr_with_comma : ',' expr;
expr_list : '[' expr ']'
          | '[' expr expr_with_comma+ ']';

// Predicates
predicate : expr '==' expr # exprEq
          | expr '>' expr  # exprGt
          | expr '<' expr  # exprLt
          | expr '!=' expr # exprNe
          | predicate AND predicate # predAnd
          | predicate OR predicate # predOr
          | '(' predicate ')' # predParen
          | '!' predicate # predNot
          | TRUE # truePred
          | FALSE #falsePred
	  ;

// Aggregation functions for group by
primitive : ID '=' expr | ';' | EMIT;
if_primitive   : primitive;
else_primitive : primitive;
if_construct : IF predicate '{' if_primitive+ '}' (ELSE '{' else_primitive+ '}')?;
stmt : primitive
     | if_construct;

agg_fun : DEF agg_func '(' state_list ',' column_list ')' ':' stmt+;

// The four operators
filter    :  SELECT '*' FROM stream WHERE predicate;
project   :  SELECT expr_list FROM stream AS column_list;
groupby   :  SELECT agg_func FROM stream GROUPBY column_list;
join      :  stream JOIN stream;
// Note: The semantics of a GROUPBY are that we return all columns in the GROUPBY field
// as well as all state maintained as part of the aggregation function.

// The two types of queries
stream_query : filter | project | groupby | join;

// Single statements in the language
stream_stmt : stream '=' stream_query ';';

// Main production rule for queries
prog : (agg_fun)* (stream_stmt)+;
