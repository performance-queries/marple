package edu.mit.needlstk;
import java.util.HashMap;
import java.util.ArrayList;

public class PipeConstructor {
  private HashMap<String, Operation> depTable;
  private HashMap<String, PipeStage> stages;
  private String lastAssignedId;
  private ArrayList<PipeStage> pipe;
  private String pktLogStr = "T"; /// Operand denoting the packet log
  
  public PipeConstructor(HashMap<String, PipeStage> stages,
                         HashMap<String, Operation> depTable,
                         String lastAssignedId) {
    this.depTable = depTable;
    this.stages = stages;
    this.lastAssignedId = lastAssignedId;
  }

  public ArrayList<PipeStage> stitchPipe() {
    this.pipe = getPipes(lastAssignedId);
    return this.pipe;
  }

  private ArrayList<PipeStage> getPipes(String queryId) {
    assert (depTable.containsKey(queryId)); // queryId should be in the depTable.
    assert (stages.containsKey(queryId));   // queryId should be one of the stages.
    Operation op = depTable.get(queryId);
    ArrayList<PipeStage> stageList = new ArrayList<>();
    for (String operand: op.operands) {
      if(! operand.equals(pktLogStr)) {
        stageList.addAll(getPipes(operand));
      }
      stageList.add(stages.get(queryId));
    }
    return stageList;
  }
}
