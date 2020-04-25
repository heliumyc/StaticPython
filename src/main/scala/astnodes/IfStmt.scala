package astnodes

case class IfStmt(condition: Expression, thenBody: BlockStmt, elseBody: BlockStmt) extends Statement {

}
