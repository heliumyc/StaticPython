package astnodes

case class IfStmt(condition:Expression, thenBody:List[Statement], elseBody:List[Statement]) extends Statement {

}
