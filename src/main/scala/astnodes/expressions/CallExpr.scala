package astnodes.expressions

import astnodes.Expression

// () operator
case class CallExpr(callee:Expression, args:List[Expression]) extends AtomExpression
