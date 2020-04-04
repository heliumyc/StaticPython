package astnodes

import lexer.IdentifierToken
import astnodes.types.PyType

case class IdType(typeName: IdentifierToken) extends PyType {

}
