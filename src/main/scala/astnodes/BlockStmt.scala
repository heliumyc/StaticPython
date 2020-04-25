package astnodes

class BlockStmt(val stmts: List[Statement]) extends Statement with ScopedAst {
    override def toString: String = {
        s"BlockStmt(${stmts.mkString(", ")})"
    }
}

object BlockStmt {
    def apply(stmts: List[Statement]): BlockStmt = {
        new BlockStmt(stmts)
    }
}
