package astnodes.declarations

import astnodes.expressions.Identifier
import astnodes.{BlockStmt, Statement, TypedVar}
import astnodes.types.{FuncType, PyType, ValueType}

// if return type is not defined, then default as NoneType
case class FuncDef(funcName: Identifier, params: List[TypedVar], returnType: ValueType, funcBody: BlockStmt) extends Declaration {

    def toFuncType: FuncType = FuncType(funcName.name, params.map(_.varType), returnType)
}
