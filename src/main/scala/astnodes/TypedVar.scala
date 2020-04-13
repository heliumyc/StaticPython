package astnodes

import astnodes.expressions.Identifier
import astnodes.types.{PyType, ValueType}

case class TypedVar(identifier: Identifier, varType: ValueType) extends PyAst {

}
