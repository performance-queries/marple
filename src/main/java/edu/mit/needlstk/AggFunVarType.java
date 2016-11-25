package edu.mit.needlstk;

public enum AggFunVarType {
  STATE, // state of the aggregation function
  FIELD, // packet field, per-packet arguments to the function
  FNVAR  // internal temporary function variables
};
