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
  public static String qinHdr = "qin";
  public static String qoutHdr = "qout";
  public static String qtimeHdr = "qtime";
  public static String ingressTin = "tingress";

  public static ArrayList<String> fields = new ArrayList<>(Arrays.asList(
      switchHdr,
      uidHdr,
      srcipHdr,
      dstipHdr,
      srcportHdr,
      dstportHdr,
      tcpseqHdr,
      protoHdr,
      inportHdr,
      outportHdr,
      payloadlenHdr,
      pktlenHdr,
      qidHdr,
      tinHdr,
      toutHdr,
      pktpathHdr,
      qinHdr,
      qoutHdr,
      qtimeHdr));
}
