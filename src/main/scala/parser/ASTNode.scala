package parser

import lexer.Token

import scala.collection.mutable

class ASTNode (val token:Token, val parent: Option[ASTNode] = None) {
  val children: mutable.ArrayBuffer[ASTNode] = mutable.ArrayBuffer[ASTNode]()

  def addChild(node: ASTNode): Unit = {
    children.addOne(node)
  }

}
