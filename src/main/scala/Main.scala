import better.files._
import File._
import java.io.{FileReader, StringReader, File => JFile}

import lexer.{EndOfFile, Lexer, NumberParser, Operator, Position, StartOfFile, Token, TokenType}

object Main {

    def main(args: Array[String]): Unit = {
        implicit val position: Position = Position()
        val reader = new FileReader("/Users/yucong/chocopy.py")
        val lexer = new Lexer(reader)
        var token:Token = Token(StartOfFile)
        while ({token = lexer.getToken; token != Token(EndOfFile)}) {
            println(token)
        }
    }
}
