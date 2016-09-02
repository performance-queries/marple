grammar perf_query;

// Skip whitespace
WS : [ \n\t\r]+ -> skip;

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
EMIT   : 'emit';
TRUE   : 'true'  | 'TRUE';
FALSE  : 'false' | 'FALSE';
INFINITY : 'INFINITY' | 'infinity';

// Identifiers
ID : ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*;
VALUE : [0-9]+;

// alias ID to stream, column, agg_func and relations
// reuses the parser for type checking as well
stream : ID;
column : ID;
agg_func : ID;
relation : ID;
// Column list
column_with_comma : ',' column;
column_list : '[' column ']'
            | '*'
	    | '[' ']'
            | '[' column column_with_comma+ ']';

// Expressions
expr : column
     | VALUE
     | INFINITY
     | expr '+' expr
     | expr '-' expr
     | expr '*' expr
     | expr '/' expr
     | '(' expr ')';

// Expression list
expr_with_comma : ',' expr;
expr_list : '[' expr ']'
          | '[' expr expr_with_comma+ ']';


// Predicates or filters
predicate : expr '==' expr
          | expr '>' expr
          | expr '<' expr
          | expr '!=' expr
          | predicate '&&' predicate
          | predicate '||' predicate
          | '(' predicate ')'
          | '!' predicate
          | TRUE
          | FALSE;

// Aggregation functions for group by
stmt : column '=' expr
     | ';'
     | EMIT
     | IF predicate THEN '{' stmt+ '}' (ELSE '{' stmt+ '}' )?;

agg_fun : DEF agg_func '(' column_list ',' column_list ')' ':' stmt+;

// Main production rule for queries
prog : (agg_fun)* ((stream '=' stream_query ';') | (relation '=' relational_query ';'))+;
stream_query : SELECT '*' FROM stream WHERE predicate
             | SELECT expr_list FROM stream AS column_list
             | SELECT agg_func FROM stream SGROUPBY column_list
             | stream JOIN stream;
relational_query : SELECT agg_func FROM stream RGROUPBY column_list;
// Note: The semantics of a S/RGROUPBY are that we return all columns in the GROUPBY field
// as well as all state maintained as part of the aggregation function.
