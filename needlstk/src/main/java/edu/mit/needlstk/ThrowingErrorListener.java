package edu.mit.needlstk;
// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.misc.*;

public class ThrowingErrorListener extends BaseErrorListener {

   @Override public void syntaxError(Recognizer<?, ?> recognizer,
                                     Object offendingSymbol,
                                     int line,
                                     int charPositionInLine,
                                     String msg,
                                     RecognitionException e)
      throws ParseCancellationException {
     throw new ParseCancellationException("\nline " + line + ":"
                                          + charPositionInLine + " " + msg);
   }
}
