package astnodes.declarations

import astnodes.ScopedAst
import astnodes.expressions.Identifier

case class ClassDef(className: Identifier, baseClass: Identifier, declarations: List[Declaration]) extends Declaration with ScopedAst
