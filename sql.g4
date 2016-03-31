grammar Sql;
query   : SELECT predicate query
        | PACKETLOG
SELECT : 'SELECT';
PACKETLOG : 'T';
predicate : field=value
          | field>value
          | field<value
          | predicate AND predicate
          | predicate  OR predicate
          | NOT predicate
field : srcip
      | dstip
value : [0-9]+
// GROUP  : 'GROUP';
// BY     : 'BY';
// PROJECT: 'PROJECT';
// AS     : 'AS';
