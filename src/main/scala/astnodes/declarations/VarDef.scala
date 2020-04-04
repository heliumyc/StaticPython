package astnodes.declarations

import astnodes.{Expression, TypedVar}

case class VarDef(typedVar: TypedVar, value: Expression) extends Declaration {

}
