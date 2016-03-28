grammar Sql;
query   : SELECT (predicate)
        | PACKETLOG
SELECT : 'SELECT';
GROUP  : 'GROUP';
BY     : 'BY';
PROJECT: 'PROJECT';
AS     : 'AS';
PACKETLOG : 'T';
