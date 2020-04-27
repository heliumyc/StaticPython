package astnodes.operators

trait Operator

object Operator {
    def operatorNameMap(op: Operator): String = op match {
        case Equal() => "__eq__"
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

    def isCoercionOp(operator: Operator): Boolean = {
        operator match {
            case Equal()|Less()|Greater()|LessEq()|GreaterEq()|Plus()|Minus()|Multiply()|Divide()|Modular()|FloorDiv()|Power() => true
            case _ => false
        }
    }
}

trait UnaryOp extends Operator

trait BinaryOp extends Operator

// comparison operator
case class Equal() extends BinaryOp {
    override def toString: String = "=="
}

case class Less() extends BinaryOp {
    override def toString: String = "<"
}

case class Greater() extends BinaryOp {
    override def toString: String = ">"
}

case class GreaterEq() extends BinaryOp {
    override def toString: String = ">="
}

case class LessEq() extends BinaryOp {
    override def toString: String = "<="
}

case class In() extends BinaryOp {
    override def toString: String = "in"
}

case class Is() extends BinaryOp {
    override def toString: String = "is"
}

case class And() extends BinaryOp {
    override def toString: String = "and"
}

case class Or() extends BinaryOp {
    override def toString: String = "or"
}

case class Not() extends UnaryOp {
    override def toString: String = "not"
}

// arithmetic operator
case class Positive() extends UnaryOp {
    override def toString: String = "+"
}

case class Negative() extends UnaryOp {
    override def toString: String = "-"
}

case class Plus() extends BinaryOp {
    override def toString: String = "+"
}

case class Minus() extends BinaryOp {
    override def toString: String = "-"
}

case class Multiply() extends BinaryOp {
    override def toString: String = "*"
}

case class Divide() extends BinaryOp {
    override def toString: String = "/"
}

case class Modular() extends BinaryOp {
    override def toString: String = "%"
}

case class FloorDiv() extends BinaryOp {
    override def toString: String = "//"
}

case class Power() extends BinaryOp {
    override def toString: String = "**"
}
