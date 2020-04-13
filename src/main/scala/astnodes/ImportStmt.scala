package astnodes

import astnodes.expressions.Identifier

case class ImportStmt(module: Identifier) extends Statement
