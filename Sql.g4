grammar Sql;

// Main production rule for queries
prog : (ID '=' query ';')+;
query : SELECT predicate (ID | PACKETLOG)
      | PROJECT field_list (ID | PACKETLOG)
      | GROUPBY field_list ',' agg_fun field_list AS field_list (ID | PACKETLOG)
      | (ID | PACKETLOG) JOIN (ID | PACKETLOG);

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
     | expr '+' expr
     | expr '-' expr
     | expr '*' expr
     | expr '/' expr;

// Fields and field lists
field : 'srcip'
      | 'dstip'
      | 'pkt_path'
      | 'qid'
      | 'tin'
      | 'tout'
      | 'qin'
      | 'qout';

field_with_comma : ',' field;
field_list : '[' field ']'
           | '[' field field_with_comma+ ']';

id_with_comma : ',' ID;
id_list : '[' ID ']'
        | '[' ID id_with_comma+ ']';


// Aggregation functions for group by
stmt : ID '=' expr
     | stmt ';' stmt;
code : stmt
     | IF predicate THEN code ELSE code;
agg_fun : 'def' ID '(' id_list ',' field_list ')' ':' code;

// Identifiers, i.e., stuff that goes into the lexer
ID : [a-z]+;
VALUE : [0-9]+ ;
WS : [ \n\t\r]+ -> skip;

// Keywords
SELECT : 'SELECT';
PROJECT : 'PROJECT';
GROUPBY : 'GROUPBY';
JOIN   : 'JOIN';
AS     : 'AS';
IF     : 'IF';
THEN   : 'THEN';
ELSE   : 'ELSE';
PACKETLOG : 'T' ;
