package astnodes.expressions

import astnodes.Expression
import common.IdentifierToken

// dot operator
case class MemberExpr(callee:Expression, member:IdentifierToken) extends AtomExpression
