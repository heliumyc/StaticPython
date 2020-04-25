package astnodes.operators

trait Operator

object Operator {
    def operatorNameMap(op: Operator): String = op match {
        case Equal() => "__eq__"
        case NotEqual() => "__ne__"
        case Less() => "__lt__"
        case Greater() => "__gt__"
        case GreaterEq() => "__ge__"
        case LessEq() => "__le__"
        case In() => "__in__"
        case Is() => "__is__"
        case And() => "__and__"
        case Or() => "__or__"
        case Not() => "__bool__" // there is no __not__, it is essentially ! __bool__()
        case Positive() => "__pos__"
        case Negative() => "__neg__"
        case Plus() => "__add__"
        case Minus() => "__sub__"
        case Multiply() => "__mul__"
        case Divide() => "__div__"
        case Modular() => "__mod__"
        case FloorDiv() => "__floordiv__"
        case Power() => "__pow__"
    }
}

trait UnaryOp extends Operator

trait BinaryOp extends Operator

// comparison operator
case class Equal() extends BinaryOp

case class NotEqual() extends BinaryOp

case class Less() extends BinaryOp

case class Greater() extends BinaryOp

case class GreaterEq() extends BinaryOp

case class LessEq() extends BinaryOp

case class In() extends BinaryOp

case class Is() extends BinaryOp

case class And() extends BinaryOp

case class Or() extends BinaryOp

case class Not() extends UnaryOp

// arithmetic operator
case class Positive() extends UnaryOp

case class Negative() extends UnaryOp

case class Plus() extends BinaryOp

case class Minus() extends BinaryOp

case class Multiply() extends BinaryOp

case class Divide() extends BinaryOp

case class Modular() extends BinaryOp

case class FloorDiv() extends BinaryOp

case class Power() extends BinaryOp
