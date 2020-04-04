package astnodes.declarations

import astnodes.{Statement, TypedVar}
import lexer.IdentifierToken
import astnodes.types.PyType

case class FuncDef(funcName: IdentifierToken, params: Seq[TypedVar], returnType: Option[PyType], funcBody: Seq[Statement]) extends Declaration {

}
