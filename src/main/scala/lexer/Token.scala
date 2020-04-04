package lexer

sealed trait PyToken extends MyPositional

case class SyntaxError(msg: String) extends PyToken
case class StartOfFile() extends PyToken
case class EndOfFile() extends PyToken
case class Empty() extends PyToken
case class NewLine() extends PyToken
case class Indent(n: Int) extends PyToken
case class Dedent() extends PyToken

case class IdentifierToken(value: String) extends PyToken

sealed trait LiteralToken extends PyToken
case class StringLiteralToken(value: String) extends LiteralToken
case class IntegerLiteralToken(value: String) extends LiteralToken
case class FloatPointLiteralToken(value: String) extends LiteralToken

sealed trait OperatorToken extends PyToken
object OperatorToken {
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
case class PERCENT() extends OperatorToken
case class AMPER() extends OperatorToken
case class LPAR() extends OperatorToken
case class RPAR() extends OperatorToken
case class STAR() extends OperatorToken
case class PLUS() extends OperatorToken
case class COMMA() extends OperatorToken
case class MINUS() extends OperatorToken
case class DOT() extends OperatorToken
case class SLASH() extends OperatorToken
case class COLON() extends OperatorToken
case class SEMI() extends OperatorToken
case class LESS() extends OperatorToken
case class EQUAL() extends OperatorToken
case class GREATER() extends OperatorToken
case class AT() extends OperatorToken
case class LSQB() extends OperatorToken
case class RSQB() extends OperatorToken
case class CIRCUMFLEX() extends OperatorToken
case class LBRACE() extends OperatorToken
case class RBRACE() extends OperatorToken
case class VBAR() extends OperatorToken
case class TILDE() extends OperatorToken

// duet
case class NOTEQUAL() extends OperatorToken
case class PERCENTEQUAL() extends OperatorToken
case class AMPEREQUAL() extends OperatorToken
case class DOUBLESTAR() extends OperatorToken
case class STAREQUAL() extends OperatorToken
case class PLUSEQUAL() extends OperatorToken
case class MINEQUAL() extends OperatorToken
case class RARROW() extends OperatorToken
case class DOUBLESLASH() extends OperatorToken
case class SLASHEQUAL() extends OperatorToken
case class COLONEQUAL() extends OperatorToken
case class LEFTSHIFT() extends OperatorToken
case class LESSEQUAL() extends OperatorToken
case class EQEQUAL() extends OperatorToken
case class GREATEREQUAL() extends OperatorToken
case class RIGHTSHIFT() extends OperatorToken
case class ATEQUAL() extends OperatorToken
case class CIRCUMFLEXEQUAL() extends OperatorToken
case class VBAREQUAL() extends OperatorToken

// triplet
case class DOUBLESTAREQUAL() extends OperatorToken
case class ELLIPSIS() extends OperatorToken
case class DOUBLESLASHEQUAL() extends OperatorToken
case class LEFTSHIFTEQUAL() extends OperatorToken
case class RIGHTSHIFTEQUAL() extends OperatorToken

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
