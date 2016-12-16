package edu.mit.needlstk;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/// Code consists of declarations and statements.
public class ThreeOpCode {
  public List<ThreeOpDecl> decls;
  public List<ThreeOpStmt> stmts;

  /// TODO: move the constant definitions below,  and the functions p4Ident and p4Value to a
  /// separate class.
  /// Default integer bitwidth used for declarations in emitted code.
  public static Integer INT_WIDTH = 32;
  /// Default boolean bitwidth used for declarations in emitted code.
  public static Integer BOOL_WIDTH = 1;
  /// Prefix used to print standard metadata fields in emitted code.
  public static String PREFIX_STANDARD_META = "standard_meta.";
  /// Prefix used to print query-related metadata fields in emitted code.
  public static String PREFIX_QUERY_META = "meta.";
  /// Prefix used to print standard headers in emitted code.
  public static String PREFIX_HEADER = "hdrs.";
  /// Placeholder prefix used to print register state in emitted code.
  public static String PREFIX_STATE = "_val_";
  // Prefix to print constant values in emitted code. Use 32 bit integers.
  public static String PREFIX_VALUE = "32w";
  // True and False literals in P4
  public static String P4_TRUE  = "true";
  public static String P4_FALSE = "false";

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
      res += "\n";
    }
    for (ThreeOpStmt stmt: stmts) {
      res += stmt.print();
      res += "\n";
    }
    return res;
  }

  /// Helper to print identifier names with the right prefix
  public static String p4Ident(String ident, AggFunVarType type) {
    switch(type) {
      case FIELD:
        if (Fields.headerFields.contains(ident)) {
          return PREFIX_HEADER + ident;
        } else if (Fields.metadataFields.contains(ident)) {
          return PREFIX_STANDARD_META + ident;
        } else { // query-defined metadata fields
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
  public static String p4Value(String value) {
    return PREFIX_VALUE + value;
  }

  public String getP4(HashMap<String, AggFunVarType> symTab) {
    String res = "";
    for (ThreeOpDecl decl: decls) {
      res += decl.getP4();
      res += "\n";
    }
    for (ThreeOpStmt stmt: stmts) {
      res += stmt.getP4(symTab);
      res += "\n";
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

  public void addDecl(ThreeOpDecl decl) {
    this.decls.add(decl);
  }
}
