package astnodes.expressions

import astnodes.Expression

case class IfExpr(condition:Expression, thenExpr:Expression, elseExpr:Expression) extends Expression
