package edu.mit.needlstk;

public enum AggFunVarType {
  STATE,    // state of the aggregation function
  FIELD,    // packet field, per-packet arguments to the function
  FN_VAR,   // internal temporary function variables
  PRED_VAR  // predicate variable, defined in code transformation
};
