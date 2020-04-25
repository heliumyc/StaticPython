package astnodes

import astnodes.expressions.Identifier

case class ForStmt(iterator:Identifier, iterable:Expression, body:BlockStmt) extends Statement
