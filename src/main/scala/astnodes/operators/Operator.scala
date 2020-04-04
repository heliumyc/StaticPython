package astnodes.operators

trait Operator

trait UnaryOp extends Operator
trait BinaryOp extends Operator

case class Less() extends BinaryOp
case class Greater() extends BinaryOp
case class Equal() extends BinaryOp
case class GreaterEq() extends BinaryOp
case class LessEq() extends BinaryOp
case class In() extends BinaryOp
case class Is() extends BinaryOp
case class And() extends BinaryOp
case class Or() extends BinaryOp
case class Not() extends UnaryOp

// arithmetic operator
case class Plus() extends UnaryOp with BinaryOp
case class Minus() extends UnaryOp with BinaryOp
case class Multiply() extends BinaryOp
case class Divide() extends BinaryOp
case class Modular() extends BinaryOp
case class FloorDiv() extends BinaryOp
case class Power() extends BinaryOp
