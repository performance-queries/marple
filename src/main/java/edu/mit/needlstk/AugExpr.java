package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class AugExpr {
  public enum AugExprType {
    EXPR_ID,
    EXPR_VAL,
    EXPR_COMB
  };

  public enum Binop {
    BINOP_ADD,
    BINOP_SUB,
    BINOP_MUL,
    BINOP_DIV
  };

  /// Enum and operator identifying the structure of the expression tree
  public AugExprType type;

  /// If the expression is just an identifier:
  public String ident;

  /// If the expression is a value: an integer or -1 (for infinity). There are only positive
  /// integers in the grammar as of now.
  public Integer value;

  /// Children expressions and combinator if this is a compound expression
  public List<AugExpr> children;
  public Binop binop;

  /// Constructor from expression in ANTLR (ExprContext)
  public AugExpr(PerfQueryParser.ExprContext ctx) {
    if (PerfQueryParser.ExprColContext.isInstance(ctx)) {
      this.type = EXPR_ID;
      this.ident = ctx.ID().getText();
    } else if (PerfQueryParser.ExprValContext.isInstance(ctx)) {
      this.type = EXPR_VAL;
      String valText = ctx.VALUE().getText();
      this.value = (valText.equals("infinity")) ? -1 : Integer.valueOf(valText);
    } else if (PerfQueryParser.ExprCombContext.isInstance(ctx)) {
      this.type = EXPR_COMB;
      this.children = MakeExprChildren(new AugExpr(ctx.expr(0)), new AugExpr(ctx.expr(1)));
      this.binop = binopFromText(ctx.op.getText());
    } else if (PerfQueryParser.ExprParenContext.isInstance(ctx)) {
      copy(new AugExpr(ctx.expr()));
    } else {
      assert(false); // Logic error. Expecting a different kind of expression?
    }
  }

  /// Constructor for specific case of expression with a supplied identifier
  public AugExpr(String id) {
    this.type = EXPR_ID;
    this.ident = id;
  }

  /// Helper to get binary operator enum from token text
  private Binop binopFromText(String txt) {
    switch(txt) {
      case "+":
        return Binop.BINOP_ADD;
      case "-":
        return Binop.BINOP_SUB;
      case "*":
        return Binop.BINOP_MUL;
      case "/":
        return Binop.BINOP_DIV;
      default:
        assert (false); // Expecting a different expression combinator?
    }
  }

  /// Helper to get printing text from binary operator type
  private String textFromBinop(Binop binop) {
    switch(binop) {
      case Binop.BINOP_ADD:
        return "+";
      case Binop.BINOP_SUB:
        return "-";
      case Binop.BINOP_MUL:
        return "*";
      case Binop.BINOP_DIV:
        return "/";
      default:
        assert (false); // Expecting a different expression combinator?
    }
  }  

  private void copy(AugExpr copySrc) {
    this.type = copySrc.type;
    this.ident = copySrc.ident;
    this.value = copySrc.value;
    this.children = copySrc.children;
    this.binop = copySrc.binop;
  }

  /// Helper to construct children list of AugExprs from two input arguments
  private List<AugExpr> MakeExprChildren(AugExpr childLeft, AugExpr childRight) {
    List<AugExpr> children = new ArrayList<AugExpr>();
    children.add(childLeft);
    children.add(childRight);
    return children;
  }

  /// Printing for inspection on console
  public String print() {
    if(type == AugExprType.EXPR_ID) {
      return ident;
    } else if(type == AugExprType.EXPR_VAL) {
      return String.valueOf(value);
    } else if(type == AugExprType.EXPR_COMB) {
      return ("(" + children.get(0).print() + ")" +
              textFromBinop(binop) +
              "(" + children.get(1).print() + ")");
    } else {
      assert (false); // Logic error. Must be one of predetermined expr types
    }
  }
}
