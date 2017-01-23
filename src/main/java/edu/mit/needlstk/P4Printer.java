package edu.mit.needlstk;
import java.util.HashMap;
import java.util.ArrayList;

public class P4Printer {
  /// Default integer bitwidth used for declarations in emitted code.
  public static Integer INT_WIDTH = 32;
  /// Default boolean bitwidth used for declarations in emitted code.
  public static Integer BOOL_WIDTH = 1;
  /// Default integer bitwidth used when left shifting in emitted code.
  public static Integer SHIFT_INT_WIDTH = 8;
  /// Type information for variables
  public static Integer INT_TYPE = 1;
  public static Integer BOOL_TYPE = 2;
  /// Prefix used to print query-related metadata fields in emitted code.
  public static String PREFIX_QUERY_META = "meta.query_meta.";
  /// Placeholder prefix used to print register state in emitted code.
  public static String PREFIX_STATE = "_val_";
  // Prefix to print constant values in emitted code. Use 32 bit integers.
  public static String MIDFIX_VALUE = "w";
  // True and False literals in P4
  public static String P4_TRUE  = "true";
  public static String P4_FALSE = "false";

  /// Map from query field name to p4 field names. These names are to be kept up to date with the
  /// fields available in the `fields` list in the class Fields.
  public static HashMap<String, String> p4Map = new HashMap<>();
  static {
    p4Map.put(Fields.switchHdr,     "switchId");
    p4Map.put(Fields.inportHdr,     "ingress_port");
    p4Map.put(Fields.outportHdr,    "egress_port");
    p4Map.put(Fields.pktlenHdr,     "packet_length");
    p4Map.put(Fields.payloadlenHdr, "payload_length");
    p4Map.put(Fields.qidHdr,        "egress_port");
    p4Map.put(Fields.tinHdr,        "enq_timestamp");
    p4Map.put(Fields.toutHdr,       "egress_timestamp");
    p4Map.put(Fields.qinHdr,        "enq_qdepth");
    p4Map.put(Fields.qoutHdr,       "deq_qdepth");
    p4Map.put(Fields.qtimeHdr,      "deq_timedelta");
    p4Map.put(Fields.uidHdr,        "identification");
    p4Map.put(Fields.srcipHdr,      "srcAddr");
    p4Map.put(Fields.dstipHdr,      "dstAddr");
    p4Map.put(Fields.srcportHdr,    "srcport");
    p4Map.put(Fields.dstportHdr,    "dstport");
    p4Map.put(Fields.tcpseqHdr,     "seqNo");
    p4Map.put(Fields.protoHdr,      "protocol");
    p4Map.put(Fields.pktpathHdr,    "pktpath");
    p4Map.put(Fields.ingressTin,    "ingress_global_timestamp");
  }

  /// List of P4 prefixes to use while reading the basic fields of the schema. These names are to be
  /// kept up to date with the fields available in the `fields` list in the class Fields.
  public static HashMap<String, String> p4PrefixMap = new HashMap<>();
  public static ArrayList<String> pktLogMetadataFields = new ArrayList<>();
  static {
    String STANDARD_TYPECAST  = "(bit<32>)";
    String META               = "meta.";
    String STANDARD_METADATA  = "standard_meta.";
    String INTRINSIC_METADATA = META + "intrinsic_metadata.";
    String QUEUE_METADATA     = META + "queueing_metadata.";
    String PKTLOG_METADATA    = META + "common_meta.";
    String HEADERS            = "hdrs.";
    String IP_PREFIX          = "ip.";
    String TCP_PREFIX         = "tcp.";
    p4PrefixMap.put(Fields.switchHdr,          PKTLOG_METADATA);
    p4PrefixMap.put(Fields.inportHdr,          STANDARD_TYPECAST + STANDARD_METADATA);
    p4PrefixMap.put(Fields.outportHdr,         STANDARD_TYPECAST + STANDARD_METADATA);
    p4PrefixMap.put(Fields.pktlenHdr,          STANDARD_TYPECAST + HEADERS + IP_PREFIX);
    p4PrefixMap.put(Fields.payloadlenHdr,      PKTLOG_METADATA);
    p4PrefixMap.put(Fields.qidHdr,             STANDARD_TYPECAST + STANDARD_METADATA);
    p4PrefixMap.put(Fields.tinHdr,             STANDARD_TYPECAST + QUEUE_METADATA);
    p4PrefixMap.put(Fields.toutHdr,            PKTLOG_METADATA);
    p4PrefixMap.put(Fields.qinHdr,             STANDARD_TYPECAST + QUEUE_METADATA);
    p4PrefixMap.put(Fields.qoutHdr,            STANDARD_TYPECAST + QUEUE_METADATA);
    p4PrefixMap.put(Fields.qtimeHdr,           QUEUE_METADATA);
    p4PrefixMap.put(Fields.uidHdr,             STANDARD_TYPECAST + HEADERS + IP_PREFIX);
    p4PrefixMap.put(Fields.srcipHdr,           HEADERS + IP_PREFIX);
    p4PrefixMap.put(Fields.dstipHdr,           HEADERS + IP_PREFIX);
    p4PrefixMap.put(Fields.srcportHdr,         PKTLOG_METADATA);
    p4PrefixMap.put(Fields.dstportHdr,         PKTLOG_METADATA);
    p4PrefixMap.put(Fields.tcpseqHdr,          HEADERS + TCP_PREFIX);
    p4PrefixMap.put(Fields.protoHdr,           STANDARD_TYPECAST + HEADERS + IP_PREFIX);
    p4PrefixMap.put(Fields.pktpathHdr,         PKTLOG_METADATA);
    p4PrefixMap.put(Fields.ingressTin,         STANDARD_TYPECAST + INTRINSIC_METADATA);
    /// For convenience, the packet-log-specific fields must be separately listed too.
    for (String f: p4PrefixMap.keySet()) {
      if (p4PrefixMap.get(f).equals(PKTLOG_METADATA)) {
        pktLogMetadataFields.add(f);
      }
    }
  }

  /// Helper to print identifier names with the right prefix
  public static String p4Ident(String ident, AggFunVarType type) {
    switch(type) {
      case FIELD:
        if (Fields.fields.contains(ident)) {
          return p4PrefixMap.get(ident) + p4Map.get(ident);
        } else {
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
