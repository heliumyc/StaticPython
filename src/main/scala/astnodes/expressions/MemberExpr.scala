package astnodes.expressions

import astnodes.Expression

// dot operator
case class MemberExpr(callee:Expression, member:Identifier) extends AtomExpression
