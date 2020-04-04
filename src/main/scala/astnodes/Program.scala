package astnodes

import parser.PyParseError

case class Program(stmtList: List[Statement], errList: List[PyParseError]) extends PyAst {

}
