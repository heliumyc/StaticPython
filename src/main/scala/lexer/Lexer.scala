package lexer

import java.io.{BufferedReader, Reader}
import utils.Util
import scala.collection.mutable

class Lexer(input: Reader) {
    private val lexeme: StringBuilder = new mutable.StringBuilder()
    private val reader: BufferedReader = new BufferedReader(input)
    private var bufferLine: String = ""
    private var ptr = 0
    private var line: Int = 0
    private val indentStack = 0 +=: mutable.ListBuffer[Int]()
    private val tokenBuffer = mutable.ListBuffer[PyToken]()
    private val escapeMap = Map('a' -> '\u0007', 'b' -> '\u0008', 'f' -> '\u000C', 'n' -> '\n', 'r' -> '\r',
        't' -> '\t', 'v' -> '\u000b', '\\' -> '\\', '\'' -> '\'', '\"' -> '\"')
    private val numberParser = new NumberParser
    private var blockStack: List[Char] = List()
    private val blockEndSet = Set(')', ']', '}')
    private val blockMap = Map('(' -> ')', '[' -> ']', '{' -> '}')
    private var isEof = false
    private var curPosition: PyPosition = PyPosition(0, 0)

    @inline
    private def nextLine: Option[String] = Option(reader.readLine())

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

    @inline
    private def pushToBuffer(x:PyToken): Unit = tokenBuffer.addOne(x.setPos(curPosition))

    @inline
    private def popDedent(): Unit = (1 to Util.removeFrontUntil[Int](indentStack, _==0)).foreach(_=>{pushToBuffer(Dedent())})

    def hasNextToken: Boolean = !isEof || tokenBuffer.nonEmpty

    def getToken: PyToken = {
        if (tokenBuffer.isEmpty)
            pushToBuffer(_getToken)
        tokenBuffer.remove(0)
    }

    @scala.annotation.tailrec
    private def _getToken: PyToken = {
        if (tokenBuffer.nonEmpty) {
            return tokenBuffer.remove(0)
        }

        lexeme.clear()
        curPosition = PyPosition(line, ptr+1) // currently ptr points to the \n
        if (isEndOfLine) {
            nextLine match {
                case Some(lineString) =>
                    bufferLine = lineString
                    line += 1
                    ptr = 0
                    if(line != 1) return NewLine()
                case None =>
                    if (indentStack.head > 0) {
                        popDedent()
                        return NewLine()
                    } else {
                        isEof = true
                        pushToBuffer(NewLine())
                        return EndOfFile()
                    }
            }
        }

        curPosition = PyPosition(line, ptr+1)
        if (isStartOfLine && !isInEnclosedBlock) {
            bufferLine.indexWhere(x => x!=' ' && x!='\t') match {
                case x if x >= 0 =>
                    // x >= 0 means there must be one char nonempty
                    ptr = x
                    if (peekNextChar().isDefined && peekNextChar().get != '#') {
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

        curPosition = PyPosition(line, ptr+1)
        peekNextChar() match {
            case None => _getToken
            case Some(c) =>
                if (c == '\"' || c == '\'') {
                    consumeNextKChar(1)
                    getString(c)
                } else if (c == '#') {
                    // ignore the rest of line if encountered comment
                    ptr = bufferLine.length
                    _getToken
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
                                return SyntaxError( s"$c does not match enclosed block")
                        }
                    }
                    getOperator(3) // opLength is possible length, actually may not as many as 3
                } else if (c == ' ' || c == '\t') {
                    // space between tokens
                    consumeNextKChar(1)
                    _getToken
                } else {
                    consumeNextKChar(1)
                    SyntaxError( "Unrecognized Symbol")
                }
        }
    }

    def checkStackAndReturn(nonEmptyIndex: Int): Option[PyToken] = {
        val indentString = bufferLine.substring(0, nonEmptyIndex)
        val indent = indentString.length + 7*indentString.count(_=='\t') // total - tab + 8*tab

        if (indentStack.head < indent) {
            indent +=: indentStack
            Some(Indent(indent))
        } else if (indentStack.head > indent) {
            if (indentStack.contains(indent)) {
                popDedent()
                // assert token buffer must has at least one, guaranteed by last if statement
                Some(tokenBuffer.remove(0))
            } else {
                Some(SyntaxError("Indentation Error"))
            }
        } else {
            None
        }
    }

    @scala.annotation.tailrec
    private def getIdentifierOrKeyword: PyToken = {
        peekNextChar() match {
            case Some(next) if next.isLetterOrDigit || next == '_' =>
                lexeme += next
                consumeNextKChar(1)
                getIdentifierOrKeyword
            case _ =>
                if (Keyword.keywords.contains(lexeme.toString())) Keyword.keywords(lexeme.toString())()
                else Identifier(lexeme.toString())
        }
    }

    @scala.annotation.tailrec
    private def getOperator(opLength: Int): PyToken = {
        // curChar is already in map, we have to peek into the next two to see whether they form a valid operator
        // this method ASSUME that the first char of dual op and triplet op in covered in single op
        if (opLength == 0) {
            SyntaxError("Unrecognized operator")
        } else {
            lookaheadChars(opLength) match {
                case None => getOperator(opLength-1)
                case Some(next) =>
                    if (Operator.operatorMap.contains(next)){
                        consumeNextKChar(opLength)
                        Operator.operatorMap(next)()
                    } else {
                        // fail on this length, backtrace
                        getOperator(opLength-1)
                    }
            }
        }
    }

    private def getString(quote: Char): PyToken = {
        var endOfString = false
        do {
            nextChar match {
                // string can only exist in one line, for any thing like new line that is read no more
                case None => return SyntaxError( "EOL while scanning string literal")
                case Some(c) if c == quote => endOfString = true
                case Some(c) =>
                    if (c == '\\') {
                        peekNextChar() match {
                            case None => return SyntaxError( "EOL while scanning string literal")
                            case Some(next) =>
                                lexeme.addOne(escapeMap.getOrElse(next, '\\'))
                                if (escapeMap.contains(next)) nextChar
                        }
                    } else {
                        lexeme += c
                    }
            }
        } while (!endOfString)
        StringLiteral(lexeme.toString())
    }

    private def getNumber: PyToken = {
        numberParser.parse(bufferLine.substring(ptr)) match {
            case (v, 1) =>
                consumeNextKChar(v.length)
                IntegerLiteral(v)
            case (v, 2) =>
                consumeNextKChar(v.length)
                FloatPointLiteral(v)
            case _ => SyntaxError( "Invalid number")
        }
    }
}
