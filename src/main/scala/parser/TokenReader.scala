package parser

import common.PyToken
import lexer.Lexer

import scala.collection.mutable

class TokenReader(val lexer: Lexer) {
    private val toReadQueue: mutable.ListBuffer[PyToken] = new mutable.ListBuffer[PyToken]()
    def peek(k: Int = 1): Option[PyToken] = {
        while (lexer.hasNextToken && toReadQueue.size < k) {
            toReadQueue.append(lexer.getToken)
        }
        if (toReadQueue.size >= k)
            Some(toReadQueue(k-1))
        else
            None
    }

    def nonEmpty(): Boolean = lexer.hasNextToken

    def consume(): Option[PyToken] = {
        if (toReadQueue.nonEmpty) {
            Some(toReadQueue.remove(0))
        } else {
            None
        }
    }

//    def drop(): Unit = {
//        buffer = Nil
//    }
//
//    def revert(): Unit = {
//        toReadQueue = buffer ::: toReadQueue
//    }
}
