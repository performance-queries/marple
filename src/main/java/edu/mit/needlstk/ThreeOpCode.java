package edu.mit.needlstk;
import java.util.List;
import java.util.ArrayList;

/// Code consists of declarations and statements.
public class ThreeOpCode {
  List<ThreeOpDecl> decls;
  List<ThreeOpStmt> stmts;

  /// Constructor
  public ThreeOpCode(ArrayList<ThreeOpDecl> decls, ArrayList<ThreeOpStmt> stmts) {
    this.decls = decls;
    this.stmts = stmts;
  }

  /// Merge the argument TOC with the current TOC, and return a new TOC.
  public orderedMerge(ThreeOpCode other) {
    return new ThreeOpCode(decls.addAll(other.decls), stmts.addAll(other.stmts));
  }
}
