package lexer

import java.io.{BufferedReader, Reader}

import utils.Util

import scala.util.matching.Regex
import scala.collection.mutable

class Lexer(input: Reader) {
    private val lexeme: StringBuilder = new mutable.StringBuilder()
    private val reader: BufferedReader = new BufferedReader(input)
    private var bufferLine: String = ""
    private var ptr = 0
    private var line: Int = 0
    private val indentStack = 0 +=: mutable.ListBuffer[Int]()
    private val tokenBuffer = mutable.ListBuffer[Token]()

    private val escapeMap = Map('a' -> '\u0007', 'b' -> '\u0008', 'f' -> '\u000C', 'n' -> '\n', 'r' -> '\r',
        't' -> '\t', 'v' -> '\u000b', '\\' -> '\\', '\'' -> '\'', '\"' -> '\"')
//    private val idStringPattern: Regex = """^([a-zA-Z][a-zA-Z0-9]*)|(\[[a-zA-Z][a-zA-Z0-9]*\])""".r
    private val numberParser = new NumberParser

    private var blockStack: List[Char] = List()
    private val blockEndSet = Set(')', ']', '}')
    private val blockMap = Map('(' -> ')', '[' -> ']', '{' -> '}')

    private implicit var curPosition: Position = _

    private def nextLine: Option[String] = {
        Option(reader.readLine())
    }

    private def nextChar: Option[Char] = {
        if (ptr < bufferLine.length) {
            ptr += 1
            Some(bufferLine.charAt(ptr-1))
        } else {
            None
        }
    }

    private def consumeNextKChar(skip: Int): Unit = ptr += skip

    private def isEndOfLine = ptr >= bufferLine.length

    private def isStartOfLine = ptr == 0

    private def isInEnclosedBlock = blockStack.nonEmpty

    private def lookaheadChars(lookahead: Int): Option[String] = {
        if (ptr+lookahead <= bufferLine.length) {
            Some(bufferLine.substring(ptr, ptr+lookahead))
        } else {
            None
        }
    }

    private def peekNextChar(k: Int = 1): Option[Char] = {
        if (ptr+k-1 < bufferLine.length) {
            Some(bufferLine(ptr+k-1))
        } else {
            None
        }
    }

    @scala.annotation.tailrec
    final def getToken: Token = {
        if (tokenBuffer.nonEmpty) {
            return tokenBuffer.remove(0)
        }

        lexeme.clear()
        if (isEndOfLine) {
            curPosition = Position(line, ptr+1) // currently ptr points to the \n
            nextLine match {
                case Some(lineString) =>
                    bufferLine = lineString
                    ptr = 0
                    line += 1
                    return Token(NewLine)
                case None =>
                    if (indentStack.head > 0) {
                        (1 to Util.removeFrontUntil[Int](indentStack, _==0)).map(_=>{Token(Dedent) +=: tokenBuffer})
                        return Token(NewLine)
                    } else {
                        return Token(EndOfFile)
                    }
            }
        }

        curPosition = Position(line, ptr+1)
        if (isStartOfLine && !isInEnclosedBlock) {
            bufferLine.indexWhere(x => x!=' ' && x!='\t') match {
                case x if x >= 0 =>
                    // x >= 0 means there must be one char nonempty
                    ptr = x
                    if (peekNextChar().isDefined && peekNextChar() != '#') {
                        checkStackAndReturn(x) match {
                            case None  => // no space no indent do nothing
                            case Some(t) => return t
                        }
                    }
                case x if x < 0 =>
                    // empty line
                    ptr = bufferLine.length
            }
        }

        curPosition = Position(line, ptr+1)
        peekNextChar() match {
            case None => getToken
            case Some(c) =>
                if (c == '\"' || c == '\'') {
                    consumeNextKChar(1)
                    getString(c)
                } else if (c == '#') {
                    // ignore the rest of line if encountered comment
                    ptr = bufferLine.length
                    getToken
                } else if (c.isLetter || c == '_') {
                    getIdentifierOrKeyword
                } else if (c.isDigit) {
                    getNumber
                } else if (c == '.' && peekNextChar(2).isDefined && peekNextChar(2).get.isDigit) {
                    // special case, . can be the prefix of a number, fuck it
                    getNumber
                } else if (Operator.operatorMap.contains(c.toString)) {
                    // possible operator
                    if (blockMap.contains(c)) {
                        blockStack ::= c
                    } else if (blockEndSet.contains(c)) {
                        blockStack match {
                            case x::xs if c == blockMap(x) =>
                                blockStack = xs // pop out the stack front that matches
                            case _ =>
                                consumeNextKChar(1)
                                return Token(SyntaxError, s"$c does not match enclosed block")
                        }
                    }
                    getOperator(3) // opLength is possible length, actually may not as many as 3
                } else if (c == ' ' || c == '\t') {
                    // space between tokens
                    consumeNextKChar(1)
                    getToken
                } else {
                    consumeNextKChar(1)
                    Token(SyntaxError, "Unrecognized Symbol")
                }
        }
    }

    def checkStackAndReturn(nonEmptyIndex: Int): Option[Token] = {
        val indentString = bufferLine.substring(0, nonEmptyIndex)
        val indent = indentString.length + 7*indentString.count(_=='\t') // total - tab + 8*tab

        if (indentStack.head < indent) {
            indent +=: indentStack
            Some(Token(Indent))
        } else if (indentStack.head > indent) {
            if (indentStack.contains(indent)) {
                (1 to Util.removeFrontUntil[Int](indentStack, _==indent)).map(_=>{Token(Dedent) +=: tokenBuffer})
                // assert token buffer must has at least one, guaranteed by last if statement
                Some(tokenBuffer.remove(0))
            } else {
                Some(Token(SyntaxError, "Indentation Error"))
            }
        } else {
            None
        }
    }

    @scala.annotation.tailrec
    private def getIdentifierOrKeyword: Token = {
        peekNextChar() match {
            case Some(next) if next.isLetterOrDigit || next == '_' =>
                lexeme += next
                consumeNextKChar(1)
                getIdentifierOrKeyword
            case _ =>
                if (Keyword.keywords.contains(lexeme.toString())) Token(Keyword.keywords(lexeme.toString()))
                else Token(Identifier, lexeme.toString())
        }
    }

    @scala.annotation.tailrec
    private def getOperator(opLength: Int): Token = {
        // curChar is already in map, we have to peek into the next two to see whether they form a valid operator
        // that is we use greedy strategy
        // this method ASSUME that the first char of dual op and triplet op in covered in single op
        if (opLength == 0) {
            Token(SyntaxError, "Unrecognized operator")
        } else {
            lookaheadChars(opLength) match {
                case None => getOperator(opLength-1)
                case Some(next) =>
                    if (Operator.operatorMap.contains(next)){
                        consumeNextKChar(opLength)
                        Token(Operator.operatorMap(next))
                    } else {
                        // fail on this length, backtrace
                        getOperator(opLength-1)
                    }
            }
        }
    }

    private def getString(quote: Char): Token = {
        var endOfString = false
        do {
            nextChar match {
                // string can only exist in one line, for any thing like new line that is read no more
                case None => return Token(SyntaxError, "EOL while scanning string literal")
                case Some(c) if c == quote => endOfString = true
                case Some(c) =>
                    if (c == '\\') {
                        peekNextChar() match {
                            case None => return Token(SyntaxError, "EOL while scanning string literal")
                            case Some(next) =>
                                lexeme.addOne(escapeMap.getOrElse(next, '\\'))
                                if (escapeMap.contains(next)) nextChar
                        }
                    } else {
                        lexeme += c
                    }
            }
        } while (!endOfString)

        Token(StringLiteral, lexeme.toString())
    }

    private def getNumber: Token = {
        // assert rest is not empty
        numberParser.parse(bufferLine.substring(ptr)) match {
            case (_, -1) => Token(SyntaxError, "Invalid number")
            case (v, 1) =>
                consumeNextKChar(v.length)
                Token(IntegerLiteral, v)
            case (v, 2) =>
                consumeNextKChar(v.length)
                Token(FloatPointLiteral, v)
        }
    }
}
