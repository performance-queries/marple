package edu.mit.needlstk;
import java.util.HashMap;

public class DominoPrinter {
  /// Prefix used to print metadata fields
  public static String PREFIX_PACKET_FIELD = "pkt.";
  /// True and false prints
  public static String DOMINO_TRUE  = "1";
  public static String DOMINO_FALSE = "0";

  /// Map from query field name to domino field names
  public static HashMap<String, String> dominoMap = new HashMap<>();
  static {
    for (String field: Fields.fields) { // domino prints all identifiers by same name
      dominoMap.put(field, field);
    } // except the switch:
    dominoMap.put(Fields.switchHdr, "switch_hdr");
  }

  public static String getDominoVarName(String ident) {
    return dominoMap.containsKey(ident) ? dominoMap.get(ident) : ident;
  }

  public static String dominoIdent(String ident, AggFunVarType type) {
    switch(type) {
      case FIELD:
      case FN_VAR:
      case PRED_VAR:
        return PREFIX_PACKET_FIELD + getDominoVarName(ident);
      case STATE:
        return ident;
      default:
        assert(false); // Logic error. Expecting a different kind of identifier?
        return null;
    }
  }

  public static String dominoValue(String value) {
    if (! value.equals("-1")) {
      return value;
    } else { /// TODO: this is a temp hack to stop domino complaining about unary minus.
      return "(0-1)";
    }
  }
}
