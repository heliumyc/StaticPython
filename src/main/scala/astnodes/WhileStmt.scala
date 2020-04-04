package astnodes

case class WhileStmt(condition:Expression, body:List[Statement]) extends Statement
