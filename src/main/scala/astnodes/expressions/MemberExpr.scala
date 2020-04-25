package astnodes.expressions

import astnodes.{Assignable, Expression}

// dot operator
case class MemberExpr(callee:Expression, member:Identifier) extends AtomExpression with Assignable
