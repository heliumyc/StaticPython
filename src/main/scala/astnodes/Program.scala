package astnodes

import common.Error

case class Program(stmtList: List[Statement], errList: List[Error]) extends PyAst {

}
