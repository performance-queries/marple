package edu.mit.needlstk;

public class DominoPrinter {
  /// Prefix used to print metadata fields
  public static String PREFIX_PACKET_FIELD = "pkt.";
  /// True and false prints
  public static String DOMINO_TRUE  = "1";
  public static String DOMINO_FALSE = "0";

  public static String dominoIdent(String ident, AggFunVarType type) {
    switch(type) {
      case FIELD:
      case FN_VAR:
      case PRED_VAR:
        if (Fields.p4Map.containsKey(ident)) {
          return PREFIX_PACKET_FIELD + Fields.p4Map.get(ident);
        } else {
          return PREFIX_PACKET_FIELD + ident;
        }
      case STATE:
        return ident;  
      default:
        assert(false); // Logic error. Expecting a different kind of identifier?
        return null;
    }
  }

  public static String dominoValue(String value) {
    return value;
  }
}
