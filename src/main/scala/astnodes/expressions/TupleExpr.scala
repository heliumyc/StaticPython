package astnodes.expressions

import astnodes.Expression

case class TupleExpr(vars:List[Expression]) extends Expression
