package lexer

case class PyPosition(line:Int, column:Int) {
    override def toString: String = s"<$line, $column>"
}

object NoPosition extends PyPosition(-1, -1)
