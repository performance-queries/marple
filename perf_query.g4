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
PKTLOG : 'T' ;
EMIT   : 'emit';

// Fields
field : 'srcip'
      | 'dstip'
      | 'srcport'
      | 'dstport'
      | 'proto'
      | 'pkt_path'
      | 'pkt_len'
      | 'payload_len'
      | 'tcpseq'
      | 'qid'
      | 'tin'
      | 'tout'
      | 'qin'
      | 'qout'
      | 'uid';

// Field list
field_with_comma : ',' field;
field_list : '[' field ']'
           | '[' field field_with_comma+ ']';

// Identifiers
ID : ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*;
VALUE : [0-9]+ ;

// column names and table names
table : PKTLOG | ID;
column : field | ID;

// Id list
id_with_comma : ',' ID;
id_list : '[' ID ']'
        | '[' ID id_with_comma+ ']';

// Column list
column_with_comma : ',' column;
column_list : '[' column ']'
            | '*'
            | '[' column column_with_comma+ ']';

// Expressions
expr : ID
     | VALUE
     | field
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
          | '!' predicate ;

// Aggregation functions for group by
stmt : ID '=' expr
     | ';'
     | EMIT
     | IF predicate THEN stmt+ (ELSE stmt+)?;

agg_fun : DEF ID '(' id_list ',' column_list ')' ':' stmt+;

// Main production rule for queries
prog : (agg_fun)* (ID '=' query ';')+;
query : SELECT column_list FROM table (WHERE predicate)?
      | SELECT expr_list FROM table AS column_list
      | SELECT column_list GROUPBY column_list FROM table
      | table JOIN table;
