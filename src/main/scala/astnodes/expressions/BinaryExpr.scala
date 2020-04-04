package astnodes.expressions

import astnodes.Expression
import astnodes.operators.BinaryOp

case class BinaryExpr(op: BinaryOp, leftExpr: Expression, rightExpr: Expression) extends Expression {

}
