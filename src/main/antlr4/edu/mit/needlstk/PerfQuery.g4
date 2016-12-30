grammar PerfQuery;

// Hide whitespace, but don't skip it
WS : [ \n\t\r]+ -> channel(HIDDEN);

// Keywords
FILTER   : 'filter';
MAP      : 'map';
GROUPBY  : 'groupby';
ZIP      : 'zip';
IF       : 'if';
THEN     : 'then';
ELSE     : 'else';
DEF      : 'def';
EMIT     : 'emit()';
TRUE     : 'true';
FALSE    : 'false';
AND      : 'and';
OR       : 'or';
ASSOC    : 'assoc';

// Values
VALUE : [0-9]+ | 'infinity';

// Identifiers
ID : ('a'..'z' | 'A'..'Z') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*;

// alias ID to stream, column, aggFunc and relations
// reuses the parser for type checking as well
state    : ID;
stream   : ID;
aggFunc  : ID;
column   : ID;

// Column list
columnWithComma : ',' column;
columnList : '[' column ']' #oneColsList
	   | '[' ']'        #noColsList
           | '[' column columnWithComma+ ']' #mulColsList
           ;

// List of state variables
stateWithComma : ',' state;
stateList : '[' state ']'
           | '[' ']'
           | '[' state stateWithComma+ ']';

// Expressions
expr : ID       # exprCol
     | VALUE    # exprVal
     | expr op=('+'|'-'|'*'|'/') expr # exprComb
     | '(' expr ')' # exprParen
     ;

// Expression list
exprWithComma : ',' expr;
exprList : '[' expr ']'
         | '[' expr exprWithComma+ ']';

// Predicates
predicate : expr '==' expr # exprEq
          | expr '>' expr  # exprGt
          | expr '<' expr  # exprLt
          | expr '!=' expr # exprNe
          | predicate AND predicate # predAnd
          | predicate OR predicate # predOr
          | '(' predicate ')' # predParen
          | '!' predicate # predNot
          | TRUE # truePred
          | FALSE #falsePred
	  ;

// Aggregation functions for group by
// primitive : ID '=' expr | ';' | EMIT;
// ifPrimitive    : primitive | ifConstruct;
// elsePrimitive  : primitive | ifConstruct;
// ifConstruct    : IF predicate '{' ifPrimitive+ '}' (ELSE '{' elsePrimitive+ '}')?;
// stmt : primitive
//      | ifConstruct;

// aggFun : DEF ASSOC? aggFunc '(' stateList ',' columnList ')' ':' stmt+;

// Aggregation functions for group by
primitive       : ID '=' expr | ';' | EMIT;
codeBlock       : primitive* ifConstruct primitive*
                | primitive+;
ifCodeBlock   : codeBlock;
elseCodeBlock : codeBlock;
ifConstruct   : IF predicate '{' ifCodeBlock '}' (ELSE '{' elseCodeBlock '}')?;

aggFun: DEF ASSOC? aggFunc '(' stateList ',' columnList ')' ':' codeBlock;

// The four operators
filter    :  FILTER '(' stream ',' predicate ')';
map       :  MAP '(' stream ',' columnList ',' exprList ')';
groupby   :  GROUPBY '(' stream ',' columnList ',' aggFunc ')';
zip       :  ZIP '(' stream ',' stream ')';
// Note: The semantics of a GROUPBY are that we return all columns in the GROUPBY field
// as well as all state maintained as part of the aggregation function.

// The two types of queries
streamQuery : filter | map | groupby | zip;

// Single statements in the language
streamStmt : stream '=' streamQuery ';';

// Main production rule for queries
prog : (aggFun)* (streamStmt)+;
