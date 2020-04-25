package astnodes.expressions

import astnodes.{Assignable, Expression}

case class IndexExpr(callee:Expression, index:Expression) extends AtomExpression with Assignable
