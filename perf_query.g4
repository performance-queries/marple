grammar perf_query;

// Skip whitespace
WS : [ \n\t\r]+ -> skip;

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
EMIT   : 'emit';
TRUE   : 'true'  | 'TRUE';
FALSE  : 'false' | 'FALSE';
INFINITY : 'INFINITY' | 'infinity';

// Identifiers
ID : ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*;
VALUE : [0-9]+;

// column names and stream names
stream : ID;
column : ID;

// Column list
column_with_comma : ',' column;
column_list : '[' column ']'
            | '*'
	    | '[' ']'
            | '[' column column_with_comma+ ']';

// Expressions
expr : ID
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
stmt : ID '=' expr
     | ';'
     | EMIT
     | IF predicate THEN '{' stmt+ '}' (ELSE '{' stmt+ '}' )?;

agg_fun : DEF ID '(' column_list ',' column_list ')' ':' stmt+;

// Main production rule for queries
prog : (agg_fun)* (ID '=' query ';')+;
query : SELECT '*' FROM stream WHERE predicate
      | SELECT expr_list FROM stream AS column_list
      | SELECT ID FROM stream GROUPBY column_list
      | stream JOIN stream;
