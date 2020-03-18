package lexer

import scala.runtime.ScalaRunTime
import scala.util.parsing.input.Positional

sealed trait PyToken extends Positional

case class SyntaxError(msg: String) extends PyToken
case class StartOfFile() extends PyToken
case class EndOfFile() extends PyToken
case class Empty() extends PyToken
case class NewLine() extends PyToken
case class Indent(n: Int) extends PyToken
case class Dedent() extends PyToken

case class Identifier(value: String) extends PyToken

sealed trait Literal extends PyToken
case class StringLiteral(value: String) extends Literal
case class IntegerLiteral(value: String) extends Literal
case class FloatPointLiteral(value: String) extends Literal

sealed trait Operator extends PyToken
object Operator {
    /*
     */
    // python makes != and <> the same thing
    val operatorMap = Map(
        // single op
        "%" -> PERCENT,
        "&" -> AMPER,
        "(" -> LPAR,
        ")" -> RPAR,
        "*" -> STAR,
        "+" -> PLUS,
        "," -> COMMA,
        "-" -> MINUS,
        "." -> DOT,
        "/" -> SLASH,
        ":" -> COLON,
        ";" -> SEMI,
        "<" -> LESS,
        "=" -> EQUAL,
        ">" -> GREATER,
        "@" -> AT,
        "[" -> LSQB,
        "]" -> RSQB,
        "^" -> CIRCUMFLEX,
        "{" -> LBRACE,
        "|" -> VBAR,
        "}" -> RBRACE,
        "~" -> TILDE,

        // dual op
        "!=" -> NOTEQUAL,
        "%=" -> PERCENTEQUAL,
        "&=" -> AMPEREQUAL,
        "**" -> DOUBLESTAR,
        "*=" -> STAREQUAL,
        "+=" -> PLUSEQUAL,
        "-=" -> MINEQUAL,
        "->" -> RARROW,
        "//" -> DOUBLESLASH,
        "/=" -> SLASHEQUAL,
        ":=" -> COLONEQUAL,
        "<<" -> LEFTSHIFT,
        "<=" -> LESSEQUAL,
        "<>" -> NOTEQUAL,
        "==" -> EQEQUAL,
        ">=" -> GREATEREQUAL,
        ">>" -> RIGHTSHIFT,
        "@=" -> ATEQUAL,
        "^=" -> CIRCUMFLEXEQUAL,
        "|=" -> VBAREQUAL,

        // triple
        "**=" -> DOUBLESTAREQUAL,
        "..." -> ELLIPSIS,
        "//=" -> DOUBLESLASHEQUAL,
        "<<=" -> LEFTSHIFTEQUAL,
        ">>=" -> RIGHTSHIFTEQUAL
    )
}

// single
case class PERCENT() extends Operator
case class AMPER() extends Operator
case class LPAR() extends Operator
case class RPAR() extends Operator
case class STAR() extends Operator
case class PLUS() extends Operator
case class COMMA() extends Operator
case class MINUS() extends Operator
case class DOT() extends Operator
case class SLASH() extends Operator
case class COLON() extends Operator
case class SEMI() extends Operator
case class LESS() extends Operator
case class EQUAL() extends Operator
case class GREATER() extends Operator
case class AT() extends Operator
case class LSQB() extends Operator
case class RSQB() extends Operator
case class CIRCUMFLEX() extends Operator
case class LBRACE() extends Operator
case class RBRACE() extends Operator
case class VBAR() extends Operator
case class TILDE() extends Operator

// duet
case class NOTEQUAL() extends Operator
case class PERCENTEQUAL() extends Operator
case class AMPEREQUAL() extends Operator
case class DOUBLESTAR() extends Operator
case class STAREQUAL() extends Operator
case class PLUSEQUAL() extends Operator
case class MINEQUAL() extends Operator
case class RARROW() extends Operator
case class DOUBLESLASH() extends Operator
case class SLASHEQUAL() extends Operator
case class COLONEQUAL() extends Operator
case class LEFTSHIFT() extends Operator
case class LESSEQUAL() extends Operator
case class EQEQUAL() extends Operator
case class GREATEREQUAL() extends Operator
case class RIGHTSHIFT() extends Operator
case class ATEQUAL() extends Operator
case class CIRCUMFLEXEQUAL() extends Operator
case class VBAREQUAL() extends Operator

// triplet
case class DOUBLESTAREQUAL() extends Operator
case class ELLIPSIS() extends Operator
case class DOUBLESLASHEQUAL() extends Operator
case class LEFTSHIFTEQUAL() extends Operator
case class RIGHTSHIFTEQUAL() extends Operator

sealed trait Keyword extends PyToken
object Keyword {
    val keywords = Map(
        "False" -> FALSE,
        "None" -> NONE,
        "True" -> TRUE,
        "and" -> AND,
        "as" -> AS,
        "assert" -> ASSERT,
        "async" -> ASYNC,
        "await" -> AWAIT,
        "break" -> BREAK,
        "class" -> CLASS,
        "continue" -> CONTINUE,
        "def" -> DEF,
        "del" -> DEL,
        "elif" -> ELIF,
        "else" -> ELSE,
        "except" -> EXCEPT,
        "finally" -> FINALLY,
        "for" -> FOR,
        "from" -> FROM,
        "global" -> GLOBAL,
        "if" -> IF,
        "import" -> IMPORT,
        "in" -> IN,
        "is" -> IS,
        "lambda" -> LAMBDA,
        "nonlocal" -> NONLOCAL,
        "not" -> NOT,
        "or" -> OR,
        "pass" -> PASS,
        "raise" -> RAISE,
        "return" -> RETURN,
        "try" -> TRY,
        "while" -> WHILE,
        "with" -> WITH,
        "yield" -> YIELD
    )
}

case class FALSE() extends Keyword
case class NONE() extends Keyword
case class TRUE() extends Keyword
case class AND() extends Keyword
case class AS() extends Keyword
case class ASSERT() extends Keyword
case class ASYNC() extends Keyword
case class AWAIT() extends Keyword
case class BREAK() extends Keyword
case class CLASS() extends Keyword
case class CONTINUE() extends Keyword
case class DEF() extends Keyword
case class DEL() extends Keyword
case class ELIF() extends Keyword
case class ELSE() extends Keyword
case class EXCEPT() extends Keyword
case class FINALLY() extends Keyword
case class FOR() extends Keyword
case class FROM() extends Keyword
case class GLOBAL() extends Keyword
case class IF() extends Keyword
case class IMPORT() extends Keyword
case class IN() extends Keyword
case class IS() extends Keyword
case class LAMBDA() extends Keyword
case class NONLOCAL() extends Keyword
case class NOT() extends Keyword
case class OR() extends Keyword
case class PASS() extends Keyword
case class RAISE() extends Keyword
case class RETURN() extends Keyword
case class TRY() extends Keyword
case class WHILE() extends Keyword
case class WITH() extends Keyword
case class YIELD() extends Keyword
