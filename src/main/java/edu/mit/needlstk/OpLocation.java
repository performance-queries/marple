package edu.mit.needlstk;
import java.util.HashSet;

/// A class to annotate operators with (i) the switches where they look at their streaming inputs;
/// and (ii) conceptually the type of stream inputs they are operating on.
public class OpLocation {
  public HashSet<Integer> switchSet;
  public StreamType streamType;

  public OpLocation(HashSet<Integer> switchSet, StreamType streamType) {
    this.switchSet = switchSet;
    this.streamType = streamType;
  }

  public OpLocation() {
    this.switchSet = new SwitchSet().getSwitches();
    this.streamType = StreamType.MULTI_SWITCH_STREAM;
  }

  public HashSet<Integer> getSwitchSet() {
    return switchSet;
  }

  public StreamType getStreamType() {
    return streamType;
  }

  public String toString() {
    return "Switch set:" + switchSet.toString() + "\nStream type:" + streamType.toString();
  }

  public String toConciseString() {
    return switchSet.size() + " switches; " + streamType.toString();
  }
}
