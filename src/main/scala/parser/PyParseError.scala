package parser

import lexer.PyPosition

case class PyParseError(msg: String, pos: PyPosition) {

}
