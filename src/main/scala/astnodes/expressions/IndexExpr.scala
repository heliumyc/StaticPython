package astnodes.expressions

import astnodes.Expression

case class IndexExpr(callee:Expression, index:Expression) extends AtomExpression
