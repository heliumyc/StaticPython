package astnodes

import astnodes.types.PyType
import common.IdentifierToken

case class TypedVar(identifier: IdentifierToken, varType: PyType) extends PyAst {

}
