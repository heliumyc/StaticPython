package lexer

case class PyPosition(line: Int, column: Int) extends {

    override def toString: String = s"<$line, $column>"

}
