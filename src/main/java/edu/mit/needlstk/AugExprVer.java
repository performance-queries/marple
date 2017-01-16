package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Objects;

/// A type of augmented expression with version numbers for identifiers.
public class AugExprVer extends AugExpr {
  /// Identifiers can take on an optional non-negative version number. Negative version numbers are
  /// used to imply that the identifier isn't versioned at all.
  public Integer identVersion;

  /// Possible children are now versioned too.
  public List<AugExprVer> children;

  /// Constructors for expressions with identifier version numbers
  public AugExprVer(PerfQueryParser.ExprContext ctx, Integer version) {
    if (ctx instanceof PerfQueryParser.ExprColContext) {
      this.type = AugExprType.EXPR_ID;
      PerfQueryParser.ExprColContext newCtx = (PerfQueryParser.ExprColContext)ctx;
      this.ident = newCtx.ID().getText();
      this.identVersion = version;
    } else if (ctx instanceof PerfQueryParser.ExprValContext) {
      this.type = AugExprType.EXPR_VAL;
      PerfQueryParser.ExprValContext newCtx = (PerfQueryParser.ExprValContext)ctx;
      String valText = newCtx.VALUE().getText();
      this.value = (valText.equals("infinity")) ? -1 : Integer.valueOf(valText);
      this.width = DEFAULT_VAL_WIDTH;
    } else if (ctx instanceof PerfQueryParser.ExprCombContext) {
      this.type = AugExprType.EXPR_COMB;
      PerfQueryParser.ExprCombContext newCtx = (PerfQueryParser.ExprCombContext)ctx;
      this.children = new ArrayList<AugExprVer>(Arrays.asList(
          new AugExprVer(newCtx.expr(0), version),
          new AugExprVer(newCtx.expr(1), version)));
      this.binop = binopFromText(newCtx.op.getText());
    } else if (ctx instanceof PerfQueryParser.ExprParenContext) {
      PerfQueryParser.ExprParenContext newCtx = (PerfQueryParser.ExprParenContext)ctx;
      copy(new AugExprVer(newCtx.expr(), version));
    } else {
      assert (false); // Logic error. Expecting a different kind of expression?
    }
  }

  public AugExprVer(String id, Integer version) {
    super(id);
    this.type = AugExprType.EXPR_ID;
    this.identVersion = version;
  }

  /// Other default constructors
  public AugExprVer(Integer val) {
    super(val);
  }

  public AugExprVer(Integer val, Integer width) {
    super(val, width);
  }

  /// Constructor to get a combinational expression with a binop
  public AugExprVer(ArrayList<AugExprVer> children, Binop binop) {
    this.type = AugExprType.EXPR_COMB;
    this.children = children;
    this.binop = binop;
  }

  /// Copy constructor, but through a copy function call
  private void copy(AugExprVer copySrc) {
    this.type = copySrc.type;
    this.ident = copySrc.ident;
    this.identVersion = copySrc.identVersion;
    this.value = copySrc.value;
    this.width = copySrc.width;
    this.children = copySrc.children;
    this.binop = copySrc.binop;
  }

  /// Substitute a given identifier with its version by a given versioned expression. Modifies the
  /// current expression's AST.
  public static AugExprVer subst(AugExprVer givenExpr,
                                 AugExprVer substExpr,
                                 String substId,
                                 Integer substVersion) {
    AugExprType type = givenExpr.type;
    String ident = givenExpr.ident;
    Integer identVersion = givenExpr.identVersion;
    if (type == AugExprType.EXPR_ID && ident.equals(substId) && identVersion == substVersion) {
      return substExpr;
    } else if (type == AugExprType.EXPR_VAL) {
      return givenExpr;
    } else if (type == AugExprType.EXPR_COMB) {
      ArrayList<AugExprVer> newChildren = new ArrayList<>();
      for (AugExprVer child: givenExpr.children) {
        newChildren.add(subst(child, substExpr, substId, substVersion));
      }
      return new AugExprVer(newChildren, givenExpr.binop);
    } else {
      assert (false); // Logic error. Expecting one of three kinds of expressions
      return null;
    }
  }

  /// Printing for inspection on console
  public String printWithVersion() {
    if (type == AugExprType.EXPR_ID) {
      return ident + ((identVersion >= 0) ? String.format("_%d", identVersion) : "");
    } else {
      return super.print();
    }
  }

  // TODO: getP4() and getDomino() don't have versioned implementations.

  /// An internal class to represent variable identifiers with their version.
  public class VarWithVersion {
    String var;
    Integer version;

    public VarWithVersion(String var, Integer version) {
      this.var = var;
      this.version = version;
    }

    @Override public boolean equals(Object oth) {
      if (oth == this) return true;
      if (! (oth instanceof VarWithVersion)) return false;
      VarWithVersion other = (VarWithVersion)oth;
      return other.var.equals(this.var) && other.version == this.version;
    }

    @Override public int hashCode() {
      return Objects.hash(var, version);
    }
  }

  public HashSet<VarWithVersion> getUsedVarsWithVersion() {
    switch (type) {
      case EXPR_ID:
        return new HashSet<>(Arrays.asList(new VarWithVersion(this.ident, this.identVersion)));
      case EXPR_VAL:
        return new HashSet<>();
      case EXPR_COMB:
        HashSet<VarWithVersion> usedVars = children.get(0).getUsedVarsWithVersion();
        usedVars.addAll(children.get(1).getUsedVarsWithVersion());
        return usedVars;
      default:
        assert (false);
        return null;
    }
  }
}
