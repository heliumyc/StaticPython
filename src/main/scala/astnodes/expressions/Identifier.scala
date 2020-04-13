package astnodes.expressions

import astnodes.Expression
import common.IdentifierToken

case class Identifier(name:String) extends Expression {

}

object Identifier {
    def fromToken(tok: IdentifierToken): Identifier = {
        Identifier(tok.value).setPos(tok.pos)
    }
}
