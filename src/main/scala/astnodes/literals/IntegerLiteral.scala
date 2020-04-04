package astnodes.literals

import astnodes.Literal

// in this type we transfer string to int value rather than in tokenizer
case class IntegerLiteral(value:Int) extends Literal
