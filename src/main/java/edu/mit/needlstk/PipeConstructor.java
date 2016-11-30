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
      addValidStmt(queryId, operandQueryId, op.opcode, stages.get(queryId));
      stageList.add(stages.get(queryId));
    }
    return stageList;
  }

  private String transformQueryId(String queryId) {
    return "0_" + queryId;
  }

  /// Given a stage of a specific type, add a "validity" statement at the end of it.
  private void addValidStmt(String queryId,
                            String operandQueryId,
                            OperationType opcode,
                            PipeStage ps) {
    outerPred = new AugPred();
    switch(opcode) {
      case FILTER:
        ps.configInfo.addValidStmt(queryId, operandQueryId);
        break;
      case PROJECT:
        ps.configInfo.addValidStmt(queryId, operandQueryId);
      case JOIN:
      case GROUPBY:
      default:
        assert(false);
        break;
    }
  }
}
