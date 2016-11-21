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
    if (ctx instanceof PerfQueryParser.ExprColContext) {
      this.type = AugExprType.EXPR_ID;
      PerfQueryParser.ExprColContext newCtx = (PerfQueryParser.ExprColContext)ctx;
      this.ident = newCtx.ID().getText();
    } else if (ctx instanceof PerfQueryParser.ExprValContext) {
      this.type = AugExprType.EXPR_VAL;
      PerfQueryParser.ExprValContext newCtx = (PerfQueryParser.ExprValContext)ctx;
      String valText = newCtx.VALUE().getText();
      this.value = (valText.equals("infinity")) ? -1 : Integer.valueOf(valText);
    } else if (ctx instanceof PerfQueryParser.ExprCombContext) {
      this.type = AugExprType.EXPR_COMB;
      PerfQueryParser.ExprCombContext newCtx = (PerfQueryParser.ExprCombContext)ctx;
      this.children = makeExprChildren(new AugExpr(newCtx.expr(0)), new AugExpr(newCtx.expr(1)));
      this.binop = binopFromText(newCtx.op.getText());
    } else if (ctx instanceof PerfQueryParser.ExprParenContext) {
      PerfQueryParser.ExprParenContext newCtx = (PerfQueryParser.ExprParenContext)ctx;
      copy(new AugExpr(newCtx.expr()));
    } else {
      assert(false); // Logic error. Expecting a different kind of expression?
    }
  }

  /// Constructor for specific case of expression with a supplied identifier
  public AugExpr(String id) {
    this.type = AugExprType.EXPR_ID;
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
      case BINOP_ADD:
        return "+";
      case BINOP_SUB:
        return "-";
      case BINOP_MUL:
        return "*";
      case BINOP_DIV:
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
  private List<AugExpr> makeExprChildren(AugExpr childLeft, AugExpr childRight) {
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
