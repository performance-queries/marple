package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;

/// A type of augmented expression with version numbers for identifiers.
public class AugExprVer extends AugExpr {
  /// Identifiers can take on an optional non-negative version number. Negative version numbers are
  /// used to imply that the identifier isn't versioned at all.
  public Integer identVersion;

  /// Constructors for expressions with identifier version numbers
  public AugExprVer(PerfQueryParser.ExprContext ctx, Integer version) {
    /// TODO: edit constructor for recursive construction of AugExprVer instead of AugExpr.
    super(ctx);
    if (this.type == AugExprType.EXPR_ID) {
      this.identVersion = version;
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

  public AugExprVer subst(String givenId, Integer givenVersion, AugExprVer expr) {
    if (type == AugExprType.EXPR_ID) {
      if (ident.equals(givenId) && identVersion == givenVersion) { // replace by the new expr
        return expr;
      } else {
        return this;
      }
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
}
