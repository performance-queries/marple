package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.Arrays;

/// List of pre-defined fields in the global packet log
public class Fields {
  public static String switchHdr = "switch";
  public static String uidHdr  = "uid";
  public static String srcipHdr = "srcip";
  public static String dstipHdr = "dstip";
  public static String srcportHdr = "srcport";
  public static String dstportHdr = "dstport";
  public static String protoHdr = "proto";
  public static String inportHdr = "inport";
  public static String outportHdr = "outport";
  public static String payloadlenHdr = "len";
  public static String pktlenHdr = "len";
  public static String qidHdr = "qid";

  public static ArrayList<String> fields = new ArrayList<>(Arrays.asList(
      switchHdr,
      uidHdr,
      srcipHdr,
      dstipHdr,
      srcportHdr,
      dstportHdr,
      protoHdr,
      inportHdr,
      outportHdr,
      payloadlenHdr,
      pktlenHdr,
      qidHdr));
}
