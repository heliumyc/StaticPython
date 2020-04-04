package astnodes.expressions

import astnodes.Expression

case class AssignExpr(target: Expression, value:Expression) extends Expression
