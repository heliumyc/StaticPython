package astnodes

import astnodes.expressions.Identifier

case class ForStmt(iterator:Identifier, iterable:Expression, body:List[Statement]) extends Statement
