package astnodes

import lexer.IdentifierToken

case class ImportStmt(module: IdentifierToken) extends Statement
