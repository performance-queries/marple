package edu.mit.needlstk;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/// Code consists of declarations and statements.
public class ThreeOpCode {
  public List<ThreeOpDecl> decls;
  public List<ThreeOpStmt> stmts;

  /// Constructor
  public ThreeOpCode(List<ThreeOpDecl> decls, List<ThreeOpStmt> stmts) {
    this.decls = decls;
    this.stmts = stmts;
  }

  /// Default constructor
  public ThreeOpCode() {
    this.decls = new ArrayList<ThreeOpDecl>();
    this.stmts = new ArrayList<ThreeOpStmt>();
  }

  /// Merge the argument TOC with the current TOC, and return a new TOC.
  public ThreeOpCode orderedMerge(ThreeOpCode other) {
    List<ThreeOpDecl> newDecls = new ArrayList<ThreeOpDecl>(decls);
    newDecls.addAll(other.decls);
    List<ThreeOpStmt> newStmts = new ArrayList<ThreeOpStmt>(stmts);
    newStmts.addAll(other.stmts);
    return new ThreeOpCode(newDecls, newStmts);
  }

  public String print() {
    String res = "";
    for (ThreeOpDecl decl: decls) {
      res += decl.print();
    }
    for (ThreeOpStmt stmt: stmts) {
      res += stmt.print();
    }
    return res;
  }

  public String getP4(HashMap<String, AggFunVarType> symTab) {
    String res = "";
    for (ThreeOpDecl decl: decls) {
      res += decl.getP4();
    }
    for (ThreeOpStmt stmt: stmts) {
      res += stmt.getP4(symTab);
    }
    return res;
  }

  public String getDomino(HashMap<String, AggFunVarType> symTab) {
    String res = "";
    // No declarations printed, as they are all collected into the packet structure.
    // Only print statements.
    for (ThreeOpStmt stmt: stmts) {
      res += stmt.getDomino(symTab);
    }
    return res;
  }

  /// Return the identifier of the first declaration in the code. Useful for things like setting up
  /// a "true" predicate at the outermost scope, and reusing that identifier everywhere.
  public String peekIdFirstDecl() {
    if(this.decls.size() > 0) {
      return this.decls.get(0).ident;
    }
    return null;
  }

  public List<ThreeOpStmt> getStmts() {
    return stmts;
  }

  public List<ThreeOpDecl> getDecls() {
    return decls;
  }

  public void appendStmt(ThreeOpStmt newStmt) {
    this.stmts.add(newStmt);
  }

  public void appendStmts(ArrayList<ThreeOpStmt> newStmts) {
    this.stmts.addAll(newStmts);
  }

  public void addDecl(ThreeOpDecl decl) {
    this.decls.add(decl);
  }

  @Override public String toString() {
    return print();
  }
}
