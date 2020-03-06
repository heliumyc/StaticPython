package lexer

case class Token(tokenType: TokenType, value: String = "")(implicit val position: Position) {
    override def toString: String = s"<$tokenType, ${position.line}, ${position.column}, $value>"
}

case class Position(line: Int = 0, column: Int = 0)

sealed trait TokenType

case object SyntaxError extends TokenType
case object StartOfFile extends TokenType
case object EndOfFile extends TokenType
case object Empty extends TokenType
case object NewLine extends TokenType
case object Indent extends TokenType
case object Dedent extends TokenType

case object Identifier extends TokenType

sealed trait Literal extends TokenType
case object StringLiteral extends Literal
//case object IdStringLiteral extends Literal
case object IntegerLiteral extends Literal
case object FloatPointLiteral extends Literal

sealed trait Operator extends TokenType
object Operator {
    /*
     */
    // python makes != and <> the same thing
    val operatorMap: Map[String, Operator] = Map[String, Operator](
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
    // single
    case object PERCENT extends Operator
    case object AMPER extends Operator
    case object LPAR extends Operator
    case object RPAR extends Operator
    case object STAR extends Operator
    case object PLUS extends Operator
    case object COMMA extends Operator
    case object MINUS extends Operator
    case object DOT extends Operator
    case object SLASH extends Operator
    case object COLON extends Operator
    case object SEMI extends Operator
    case object LESS extends Operator
    case object EQUAL extends Operator
    case object GREATER extends Operator
    case object AT extends Operator
    case object LSQB extends Operator
    case object RSQB extends Operator
    case object CIRCUMFLEX extends Operator
    case object LBRACE extends Operator
    case object RBRACE extends Operator
    case object VBAR extends Operator
    case object TILDE extends Operator

    // duet
    case object NOTEQUAL extends Operator
    case object PERCENTEQUAL extends Operator
    case object AMPEREQUAL extends Operator
    case object DOUBLESTAR extends Operator
    case object STAREQUAL extends Operator
    case object PLUSEQUAL extends Operator
    case object MINEQUAL extends Operator
    case object RARROW extends Operator
    case object DOUBLESLASH extends Operator
    case object SLASHEQUAL extends Operator
    case object COLONEQUAL extends Operator
    case object LEFTSHIFT extends Operator
    case object LESSEQUAL extends Operator
    case object EQEQUAL extends Operator
    case object GREATEREQUAL extends Operator
    case object RIGHTSHIFT extends Operator
    case object ATEQUAL extends Operator
    case object CIRCUMFLEXEQUAL extends Operator
    case object VBAREQUAL extends Operator

    // triplet
    case object DOUBLESTAREQUAL extends Operator
    case object ELLIPSIS extends Operator
    case object DOUBLESLASHEQUAL extends Operator
    case object LEFTSHIFTEQUAL extends Operator
    case object RIGHTSHIFTEQUAL extends Operator
}

sealed trait Keyword extends TokenType
object Keyword {
    val keywords: Map[String, Keyword] = Map[String, Keyword](
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

    case object FALSE extends Keyword
    case object NONE extends Keyword
    case object TRUE extends Keyword
    case object AND extends Keyword
    case object AS extends Keyword
    case object ASSERT extends Keyword
    case object ASYNC extends Keyword
    case object AWAIT extends Keyword
    case object BREAK extends Keyword
    case object CLASS extends Keyword
    case object CONTINUE extends Keyword
    case object DEF extends Keyword
    case object DEL extends Keyword
    case object ELIF extends Keyword
    case object ELSE extends Keyword
    case object EXCEPT extends Keyword
    case object FINALLY extends Keyword
    case object FOR extends Keyword
    case object FROM extends Keyword
    case object GLOBAL extends Keyword
    case object IF extends Keyword
    case object IMPORT extends Keyword
    case object IN extends Keyword
    case object IS extends Keyword
    case object LAMBDA extends Keyword
    case object NONLOCAL extends Keyword
    case object NOT extends Keyword
    case object OR extends Keyword
    case object PASS extends Keyword
    case object RAISE extends Keyword
    case object RETURN extends Keyword
    case object TRY extends Keyword
    case object WHILE extends Keyword
    case object WITH extends Keyword
    case object YIELD extends Keyword
}
