package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;

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
    BINOP_DIV,
    BINOP_RSHIFT
  };

  /// Enum and operator identifying the structure of the expression tree
  public AugExprType type;

  /// If the expression is just an identifier:
  public String ident;

  /// If the expression is a value: an integer or -1 (for infinity). There are only positive
  /// integers in the grammar as of now. Values can also have bit widths, e.g., 1 or 32.
  public Integer value;
  public Integer width;
  public static Integer DEFAULT_VAL_WIDTH = 32;

  /// Children expressions and combinator if this is a compound expression
  public List<AugExpr> children;
  public Binop binop;

  /// Internal definition of infinity.
  public static Integer EXPR_INFINITY = Integer.MAX_VALUE;

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
      this.value = (valText.equals("infinity")) ? EXPR_INFINITY : Integer.valueOf(valText);
      this.width = DEFAULT_VAL_WIDTH;
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

  /// Constructor for specific case of expression with a supplied value
  public AugExpr(Integer val) {
    this.type = AugExprType.EXPR_VAL;
    this.value = val;
    this.width = DEFAULT_VAL_WIDTH;
  }

  /// Constructor for values with specific bit widths
  public AugExpr(Integer val, Integer width) {
    this.type = AugExprType.EXPR_VAL;
    this.value = val;
    this.width = width;
  }

  /// Constructor for combinational expressions with a specified binary operator
  public AugExpr(AugExpr op1, AugExpr op2, String op) {
    if (op != "+" && op != "-" && op != "*" && op != "/") {
      throw new RuntimeException("Operators must be one of pre-specified arithmetic types:\n" +
                                 "+ - * / \n");
    }
    this.type = AugExprType.EXPR_COMB;
    this.children = new ArrayList<>(Arrays.asList(op1, op2));
    this.binop = binopFromText(op);
  }

  /// Default constructor for inheritance
  public AugExpr() { }

  /// Helper to get binary operator enum from token text
  protected Binop binopFromText(String txt) {
    switch(txt) {
      case "+":
        return Binop.BINOP_ADD;
      case "-":
        return Binop.BINOP_SUB;
      case "*":
        return Binop.BINOP_MUL;
      case "/":
        return Binop.BINOP_DIV;
      case "<<":
        return Binop.BINOP_RSHIFT;
      default:
        assert (false); // Expecting a different expression combinator?
        return null;
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
      case BINOP_RSHIFT:
        return ">>";
      default:
        assert (false); // Expecting a different expression combinator?
        return null;
    }
  }

  private void copy(AugExpr copySrc) {
    this.type = copySrc.type;
    this.ident = copySrc.ident;
    this.value = copySrc.value;
    this.width = copySrc.width;
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

  public HashSet<String> getUsedVars() {
    switch(type) {
      case EXPR_ID:
        return new HashSet<>(Arrays.asList(ident));
      case EXPR_VAL:
        return new HashSet<>();
      case EXPR_COMB:
        HashSet<String> usedVars = children.get(0).getUsedVars();
        usedVars.addAll(children.get(1).getUsedVars());
        return usedVars;
      default:
        assert(false);
        return null;
    }
  }

  /// If the expression is affine in the identifier provided, return the A and B coefficients as
  /// expressions. Otherwise, an empty list is returned.
  /// This detection is very simple and sound, although quite incomplete. The expression must either
  /// be exactly equal to:
  /// AugExpr(var) (OR)
  /// AugExpr((expr*var) + (expr)) (OR)
  /// AugExpr(value) (OR)
  /// AugExpr(var + expr)
  /// The order of the terms can move around, but the form is the same.
  /// Note that the second form only allows expressions where the var (say x) is one of the two
  /// topmost AST elements. For example, according to this function, (3*2)*x is recognized as affine
  /// in x, but (3*x)*2 is not.
  public ArrayList<AugExpr> getAffineCoefficients(String var) {
    /// AugExpr(var)
    if (this.isSingleIdentExpr(var)) {
      return new ArrayList<AugExpr>(Arrays.asList(
          new AugExpr(1),
          new AugExpr(0)));
    }
    /// AugExpr(value)
    if (this.isValueExpr()) {
      return new ArrayList<AugExpr>(Arrays.asList(
          new AugExpr(0),
          this));
    }
    /// AugExpr((expr*var) + (expr))
    if (! (type == AugExprType.EXPR_COMB)) return new ArrayList<AugExpr>();
    AugExpr child0 = this.children.get(0);
    AugExpr child1 = this.children.get(1);
    if (this.isAddExpr()) {
      if (child0.isLinearExpr(var) && ! child1.usesIdent(var)) {
        return new ArrayList<AugExpr>(Arrays.asList(
            child0.getLinearCoefficient(var),
            child1));
      } else if (child1.isLinearExpr(var) && ! child0.usesIdent(var)) {
        return new ArrayList<AugExpr>(Arrays.asList(
            child1.getLinearCoefficient(var),
            child0));
      }
    }
    return new ArrayList<AugExpr>();
  }

  /// Helpers for affine coefficient detection
  public AugExpr getLinearCoefficient(String var) {
    assert (this.isLinearExpr(var));
    if (isSingleIdentExpr(var)) return new AugExpr(1);
    else return this.children.get(0).isSingleIdentExpr(var) ?
             this.children.get(1) : this.children.get(0);
  }

  public boolean isMulExpr() {
    return (type == AugExprType.EXPR_COMB && binop == Binop.BINOP_MUL);
  }

  public boolean isAddExpr() {
    return (type == AugExprType.EXPR_COMB && binop == Binop.BINOP_ADD);
  }

  public boolean isSingleIdentExpr(String var) {
    return (type == AugExprType.EXPR_ID && ident.equals(var));
  }

  public boolean isValueExpr() {
    return (type == AugExprType.EXPR_VAL);
  }

  public boolean usesIdent(String var) {
    return this.getUsedVars().contains(var);
  }

  public boolean isLinearExpr(String var) {
    if (isSingleIdentExpr(var)) return true;
    if (isValueExpr()) return true;
    if (! (type == AugExprType.EXPR_COMB)) return false;
    AugExpr child0 = this.children.get(0);
    AugExpr child1 = this.children.get(1);
    return (this.isMulExpr() &&
            ((child0.isSingleIdentExpr(var) && ! child1.usesIdent(var)) ||
             (child1.isSingleIdentExpr(var) && ! child0.usesIdent(var))));
  }

  public boolean isAffine(String var) {
    return (getAffineCoefficients(var).size() == 2);
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
      return null;
    }
  }

  /// Print P4 code
  public String getP4(HashMap<String, AggFunVarType> symTab) {
    if(type == AugExprType.EXPR_ID) {
      /// Ensure that symbol table contains the identifier
      assert (symTab.containsKey(ident));
      return P4Printer.p4Ident(ident, symTab.get(ident));
    } else if(type == AugExprType.EXPR_VAL) {
      return P4Printer.p4Value(String.valueOf(value), width);
    } else if(type == AugExprType.EXPR_COMB) {
      return ("(" + children.get(0).getP4(symTab) + ")" +
              textFromBinop(binop) +
              "(" + children.get(1).getP4(symTab) + ")");
    } else {
      assert (false); // Logic error. Must be one of predetermined expr types
      return null;
    }
  }

  /// Print domino code
  public String getDomino(HashMap<String, AggFunVarType> symTab) {
    if(type == AugExprType.EXPR_ID) {
      /// Ensure that symbol table contains the identifier
      assert (symTab.containsKey(ident));
      return DominoPrinter.dominoIdent(ident, symTab.get(ident));
    } else if(type == AugExprType.EXPR_VAL) {
      return DominoPrinter.dominoValue(String.valueOf(value));
    } else if(type == AugExprType.EXPR_COMB) {
      return ("(" + children.get(0).getDomino(symTab) + ")" +
              textFromBinop(binop) +
              "(" + children.get(1).getDomino(symTab) + ")");
    } else {
      assert (false); // Logic error. Must be one of predetermined expr types
      return null;
    }
  }

  /// Return type of the expression
  public AugExprType getType() {
    return this.type;
  }

  @Override public String toString() {
    return print();
  }

  /// Helpers for a pass that transform expressions of divisions by powers of 2 internally to
  /// shifts.  This isn't the ideal place to do this, but seems to serve as of now. A more ideal
  /// design would use a general lambda function that can transform expressions, and the class
  /// corresponding to the pass would supply the lambda.
  private Integer getValue() {
    assert (isValueExpr());
    return this.value;
  }

  private void setValue(Integer val) {
    assert (isValueExpr());
    this.value = val;
  }

  private void setWidth(Integer width) {
    assert (isValueExpr());
    this.width = width;
  }

  private boolean isPowerOf2(Integer x) {
    return (x & (x-1)) == 0;
  }

  private Integer getPowerOf2(Integer x) {
    return new Integer((int)(Math.log(x) / Math.log(2)));
  }

  /// Helper function to transform ASTs with constant divisors which are powers of 2.
  /// Other divisions are disallowed currently.
  public void transformDivision() {
    if (type == AugExprType.EXPR_ID || type == AugExprType.EXPR_VAL) {
      // do nothing.
      return;
    } else if (type == AugExprType.EXPR_COMB) {
      if (this.binop == Binop.BINOP_DIV) {
        if (this.children.get(1).getType() == AugExprType.EXPR_VAL) {
          Integer val = this.children.get(1).getValue();
          if (isPowerOf2(val)) {
            System.out.print("Changed division expression " + this.toString());
            this.binop = Binop.BINOP_RSHIFT;
            Integer shiftCount = getPowerOf2(val);
            if (shiftCount > P4Printer.SHIFT_INT_WIDTH) {
              throw new RuntimeException("Divisor cannot be greater than " +
                                         String.valueOf(P4Printer.SHIFT_INT_WIDTH) +
                                         " bits long!");
            }
            this.children.get(1).setValue(shiftCount);
            this.children.get(1).setWidth(P4Printer.SHIFT_INT_WIDTH);
            System.out.println(" to " + this.toString());
            /// Transform the other child independently next
            this.children.get(0).transformDivision();
          } else {
            throw new RuntimeException("Divisor must be a power of 2!");
          }
        } else {
          throw new RuntimeException("Divisor must be a constant value!");
        }
      } else { // other kinds of combinators are fine. Transform independently
        this.children.get(0).transformDivision();
        this.children.get(1).transformDivision();
      }
    }
  }

}
