package edu.mit.needlstk;

public class P4Printer {
  /// Default integer bitwidth used for declarations in emitted code.
  public static Integer INT_WIDTH = 32;
  /// Default boolean bitwidth used for declarations in emitted code.
  public static Integer BOOL_WIDTH = 1;
  /// Type information for variables
  public static Integer INT_TYPE = 1;
  public static Integer BOOL_TYPE = 2;
  /// Prefix used to print standard metadata fields in emitted code.
  public static String PREFIX_STANDARD_META = "meta.common_meta.";
  /// Prefix used to print performance-related fields in the PKTLOG in emitted code.
  public static String PREFIX_PKTLOG_META = "meta.common_meta.";
  /// Prefix used to print query-related metadata fields in emitted code.
  public static String PREFIX_QUERY_META = "meta.query_meta.";
  /// Prefix used to print standard headers in emitted code.
  public static String PREFIX_HEADER = "hdrs.";
  /// Placeholder prefix used to print register state in emitted code.
  public static String PREFIX_STATE = "val.";
  // Prefix to print constant values in emitted code. Use 32 bit integers.
  public static String MIDFIX_VALUE = "w";
  // True and False literals in P4
  public static String P4_TRUE  = "true";
  public static String P4_FALSE = "false";

  /// Helper to print identifier names with the right prefix
  public static String p4Ident(String ident, AggFunVarType type) {
    switch(type) {
      case FIELD:
        if (Fields.headerFields.contains(ident)) {
          return PREFIX_HEADER + Fields.p4Map.get(ident);
        } else if (Fields.v1MetadataFields.contains(ident)) {
          return PREFIX_STANDARD_META + Fields.p4Map.get(ident);
        } else if (Fields.pktLogMetadataFields.contains(ident)) {
          return PREFIX_PKTLOG_META + Fields.p4Map.get(ident);
        } else { // query-defined metadata fields
          return PREFIX_QUERY_META + ident;
        }
      case STATE:
        return PREFIX_STATE + ident;
      case FN_VAR:
      case PRED_VAR:
        return ident;
      default:
        assert(false); // Logic error.
        return null;
    }
  }

  /// Helper to print values with the right prefix
  public static String p4Value(String value, Integer width) {
    return width + MIDFIX_VALUE + value;
  }
}
