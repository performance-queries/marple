grammar perf_query;

// Identifiers, i.e., stuff that goes into the lexer
ID : [a-z]+;
VALUE : [0-9]+ ;
WS : [ \n\t\r]+ -> skip;

// Id list
id_with_comma : ',' ID;
id_list : '[' ID ']'
        | '[' ID id_with_comma+ ']';

// Keywords
SELECT : 'SELECT' | 'select' ;
WHERE : 'WHERE' | 'where' ;
FROM : 'FROM' | 'from' ;
GROUPBY : 'GROUPBY' | 'groupby';
JOIN   : 'JOIN' | 'join';
IF     : 'IF' | 'if';
THEN   : 'THEN' | 'then';
ELSE   : 'ELSE' | 'else';
PKTLOG : 'T' ;

// Fields
field : 'srcip'
      | 'dstip'
      | 'srcport'
      | 'dstport'
      | 'proto'
      | 'pkt_path'
      | 'pkt_len'
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

// Expressions
expr : ID
     | VALUE
     | field
     | expr '+' expr
     | expr '-' expr
     | expr '*' expr
     | expr '/' expr
     | '(' expr ')';

// Predicates or filters
predicate : expr '=' VALUE
          | expr '>' VALUE
          | expr '<' VALUE
          | predicate 'AND' predicate
          | predicate  'OR' predicate
          | 'NOT' predicate ;

// Aggregation functions for group by
stmt : ID '=' expr
     | stmt ';' stmt;
code : stmt
     | IF predicate THEN code ELSE code;
agg_fun : 'def' ID '(' id_list ',' field_list ')' ':' code;

// Main production rule for queries
prog : (agg_fun)* (ID '=' query ';')+;
query : SELECT (field_list | '*') FROM (ID | PKTLOG) (WHERE predicate)?
      | SELECT ID GROUPBY field_list FROM (ID | PKTLOG)
      | (ID | PKTLOG) JOIN (ID | PKTLOG);
