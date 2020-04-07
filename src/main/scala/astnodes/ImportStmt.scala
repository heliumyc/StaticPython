package astnodes

import common.IdentifierToken

case class ImportStmt(module: IdentifierToken) extends Statement
