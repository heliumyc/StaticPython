package astnodes

case class WhileStmt(condition: Expression, body: BlockStmt) extends Statement
