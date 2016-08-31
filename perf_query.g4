grammar perf_query;

// Identifiers, i.e., stuff that goes into the lexer
ID : [a-z]+;
VALUE : [0-9]+ ;
WS : [ \n\t\r]+ -> skip;

// Keywords
SELECT : 'SELECT';
WHERE : 'WHERE';
FROM : 'FROM';
GROUPBY : 'GROUPBY';
JOIN   : 'JOIN';
AS     : 'AS';
IF     : 'IF';
THEN   : 'THEN';
ELSE   : 'ELSE';
PKTLOG : 'T' ;

// Predicates or filters
predicate : field '=' VALUE
          | field '>' VALUE
          | field '<' VALUE
          | predicate 'AND' predicate
          | predicate  'OR' predicate
          | 'NOT' predicate ;

// Expressions
expr : ID
     | VALUE
     | field
     | expr '+' expr
     | expr '-' expr
     | expr '*' expr
     | expr '/' expr;

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

// Id list
id_with_comma : ',' ID;
id_list : '[' ID ']'
        | '[' ID id_with_comma+ ']';

// Aggregation functions for group by
stmt : ID '=' expr
     | stmt ';' stmt;
code : stmt
     | IF predicate THEN code ELSE code;
agg_fun : 'def' ID '(' id_list ',' field_list ')' ':' code;

// Main production rule for queries
prog : (agg_fun)+ (ID '=' query ';')+;
query : SELECT (field_list | '*') FROM (ID | PKTLOG) (WHERE predicate)?
      | SELECT ID GROUPBY field_list FROM (ID | PKTLOG)
      | (ID | PKTLOG) JOIN (ID | PKTLOG);
