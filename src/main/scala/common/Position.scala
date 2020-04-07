package common

case class Position(line:Int, column:Int) {
    override def toString: String = s"<$line, $column>"
}

object NoPosition extends Position(-1, -1)
