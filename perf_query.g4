grammar perf_query;

// Hide whitespace, but don't skip it
WS : [ \n\t\r]+ -> channel(HIDDEN);

// Keywords
SELECT : 'SELECT' | 'select' ;
WHERE : 'WHERE' | 'where' ;
FROM : 'FROM' | 'from' ;
SGROUPBY : 'SGROUPBY' | 'sgroupby';
RGROUPBY : 'RGROUPBY' | 'rgroupby';
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
ID : ('a'..'z' | 'A'..'Z') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*;
VALUE : [0-9]+;

// alias ID to stream, column, agg_func and relations
// reuses the parser for type checking as well
state    : '_s_' ID;
stream   : ID;
agg_func : ID;
relation : ID;
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
expr : column | state | VALUE | INFINITY
     | expr '+' expr | expr '-' expr | expr '*' expr | expr '/' expr
     | '(' expr ')';

// Expression list
expr_with_comma : ',' expr;
expr_list : '[' expr ']'
          | '[' expr expr_with_comma+ ']';

// Predicates
pred : expr '==' expr | expr '>' expr | expr '<' expr | expr '!=' expr
     | pred AND pred | pred OR pred | '(' pred ')' | '!' pred
     | TRUE | FALSE;

// Aggregation functions for group by
primitive : column '=' expr | state '=' expr | ';' | EMIT;
if_primitive   : primitive;
else_primitive : primitive;
if_construct : IF pred '{' if_primitive+ '}' (ELSE '{' else_primitive+ '}')?;
stmt : primitive
     | if_construct;

agg_fun : DEF agg_func '(' state_list ',' column_list ')' ':' stmt+;

// The five operators
filter  :  SELECT '*' FROM stream WHERE pred;
project :  SELECT expr_list FROM stream AS column_list;
sfold   :  SELECT agg_func FROM stream SGROUPBY column_list;
join    :  stream JOIN stream;
rfold   :  SELECT agg_func FROM stream RGROUPBY column_list;
// Note: The semantics of a S/RGROUPBY are that we return all columns in the GROUPBY field
// as well as all state maintained as part of the aggregation function.

// The two types of queries
stream_query : filter | project | sfold | join;
relational_query : rfold;

// Single statements in the language
stream_stmt : stream '=' stream_query ';';
relational_stmt    : relation '=' relational_query ';';

// Main production rule for queries
prog : (agg_fun)* (stream_stmt | relational_stmt)+;
