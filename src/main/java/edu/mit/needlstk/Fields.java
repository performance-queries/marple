package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/// List of pre-defined fields in the global packet log
public class Fields {
  public static String switchHdr = "switch";
  public static String uidHdr  = "uid";
  public static String srcipHdr = "srcip";
  public static String dstipHdr = "dstip";
  public static String srcportHdr = "srcport";
  public static String dstportHdr = "dstport";
  public static String tcpseqHdr = "tcpseq";
  public static String protoHdr = "proto";
  public static String inportHdr = "inport";
  public static String outportHdr = "outport";
  public static String payloadlenHdr = "payloadlen";
  public static String pktlenHdr = "pktlen";
  public static String qidHdr = "qid";
  public static String tinHdr = "tin";
  public static String toutHdr = "tout";
  public static String pktpathHdr = "pktpath";
  public static String qlenHdr = "qlen";

  public static ArrayList<String> metadataFields = new ArrayList<>(Arrays.asList(
      switchHdr,
      inportHdr,
      outportHdr,
      pktlenHdr,
      payloadlenHdr,
      qidHdr,
      tinHdr,
      toutHdr,
      qlenHdr));

  public static ArrayList<String> headerFields = new ArrayList<>(Arrays.asList(
      uidHdr,
      srcipHdr,
      dstipHdr,
      srcportHdr,
      dstportHdr,
      tcpseqHdr,
      protoHdr,
      pktpathHdr));

  public static final ArrayList<String> fields = new ArrayList<>();
  static {
    fields.addAll(metadataFields);
    fields.addAll(headerFields);
  }

  /// Map from query field name to p4 field names
  public static HashMap<String, String> p4Map = new HashMap<>();
  static {
    /// Metadata fields
    p4Map.put(switchHdr,     "switch");            // TODO: corresponding p4 field?
    p4Map.put(inportHdr,     "ingress_port");
    p4Map.put(outportHdr,    "egress_port");
    p4Map.put(pktlenHdr,     "packet_length");
    p4Map.put(payloadlenHdr, "payload_length");    // TODO: corresponding p4 field?
    p4Map.put(qidHdr,        "qid");               // TODO: corresponding p4 field?
    p4Map.put(tinHdr,        "ingress_timestamp"); // TODO: corresponding p4 field?
    p4Map.put(toutHdr,       "egress_timestamp");  // TODO: corresponding p4 field?
    p4Map.put(qlenHdr,       "queue_len");         // TODO: corresponding p4 field?
    /// Header fields
    p4Map.put(uidHdr,        "ip.identification");
    p4Map.put(srcipHdr,      "ip.srcAddr");
    p4Map.put(dstipHdr,      "ip.dstAddr");
    p4Map.put(srcportHdr,    "tcp.srcport");     // TODO: parse tcp srcport
    p4Map.put(dstportHdr,    "tcp.dstport");     // TODO: parse tcp dstport
    p4Map.put(tcpseqHdr,     "tcp.sequence");    // TODO: parse tcp sequence number
    p4Map.put(protoHdr,      "ip.protocol");
    p4Map.put(pktpathHdr,    "pktpath");         // TODO: add a pktpath header?
  }
}
