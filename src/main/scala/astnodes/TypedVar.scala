package astnodes

import lexer.IdentifierToken
import astnodes.types.PyType

case class TypedVar(identifier: IdentifierToken, varType: PyType) extends PyAst {

}
