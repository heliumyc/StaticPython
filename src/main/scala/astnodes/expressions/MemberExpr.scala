package astnodes.expressions

import astnodes.Expression
import lexer.IdentifierToken

// dot operator
case class MemberExpr(callee:Expression, member:IdentifierToken) extends AtomExpression
