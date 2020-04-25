package astnodes.expressions

import astnodes.{Assignable, Expression}
import common.IdentifierToken

case class Identifier(name:String) extends Expression with Assignable {

}

object Identifier {
    def fromToken(tok: IdentifierToken): Identifier = {
        Identifier(tok.value).setPos(tok.pos)
    }
}
