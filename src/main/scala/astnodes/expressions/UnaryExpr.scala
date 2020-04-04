package astnodes.expressions

import astnodes.Expression
import astnodes.operators.UnaryOp

case class UnaryExpr(op: UnaryOp, operand: Expression) extends Expression
