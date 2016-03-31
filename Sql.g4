grammar Sql;
query   : SELECT predicate query
        | PACKETLOG ;
SELECT : 'SELECT' ;
PACKETLOG : 'T' ;
predicate : field '=' VALUE
          | field '>' VALUE
          | field '<' VALUE
          | predicate 'AND' predicate
          | predicate  'OR' predicate
          | 'NOT' predicate ;
field : 'srcip'
      | 'dstip' ;
VALUE : [0-9]+ ;
// GROUP  : 'GROUP';
// BY     : 'BY';
// PROJECT: 'PROJECT';
// AS     : 'AS';
