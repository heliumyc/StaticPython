package astnodes.declarations

import astnodes.{Statement, TypedVar}
import astnodes.types.PyType
import common.IdentifierToken

case class FuncDef(funcName: IdentifierToken, params: Seq[TypedVar], returnType: Option[PyType], funcBody: Seq[Statement]) extends Declaration {

}
