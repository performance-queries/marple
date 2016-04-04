// TODO: parentheses, rewrite fields, actions for each rule

grammar Sql;

// Main production rule for queries
prog : (query)+;
query   : SELECT predicate query
        | PROJECT field_list query
        | GROUPBY field_list ',' agg_fun field_list AS field_list query
        | PACKETLOG ;

// Predicates or filters
predicate : field '=' VALUE
          | field '>' VALUE
          | field '<' VALUE
          | predicate 'AND' predicate
          | predicate  'OR' predicate
          | 'NOT' predicate ;

// Fields and field lists
field : 'srcip'
      | 'dstip' ;

field_with_comma : ',' field;
field_list : '[' field ']'
           | '[' field field_with_comma+ ']';

// Aggregation functions for group by
agg_fun : 'LAST_MAX'
        | 'LAST_MIN'
        | 'LAST';

// Identifiers 
VALUE : [0-9]+ ;
WS: [ \n\t\r]+ -> skip;

// Keywords
SELECT : 'SELECT' ;
PROJECT : 'PROJECT' ;
GROUPBY : 'GROUPBY' ;
AS     : 'AS';
PACKETLOG : 'T' ;
