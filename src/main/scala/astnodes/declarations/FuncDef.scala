package astnodes.declarations

import astnodes.expressions.Identifier
import astnodes.{Statement, TypedVar}
import astnodes.types.{PyType, ValueType}

case class FuncDef(funcName: Identifier, params: List[TypedVar], returnType: Option[ValueType], funcBody: List[Statement]) extends Declaration {

}
