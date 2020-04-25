import better.files._
import File._
import java.io.{FileReader, StringReader, File => JFile}

import analysis.{DeclarationAnalyzer, SemanticAnalysis}
import astnodes.ForStmt
import astnodes.declarations.FuncDef
import common.PyToken
import lexer.{Lexer, NumberParser}
import parser.{PyParser, TokenReader}

object Main {

    def main(args: Array[String]): Unit = {
        // lexer
//        val reader = new FileReader("/Users/yucong/chocopy.py")
        val reader = new FileReader("/Users/yucong/tmp2.py")
        val lexer = new Lexer(reader)
        // print tokens
//        while (lexer.hasNextToken) {
//            var token: PyToken = lexer.getToken
//            println(token, token.pos)
//        }

        // parser
        val parser = new PyParser(lexer)
        val program = parser.parse()

        println(program)
        println(program.errors)

        // sem analyze
        val analysis = new SemanticAnalysis()
        analysis.process(program)

        println(program.errors)
    }
}
