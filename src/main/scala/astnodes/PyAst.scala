package astnodes

import lexer.{NoPosition, PyPosition}

trait PyAst {
    var pos:PyPosition = NoPosition
    def setPos(position: PyPosition): this.type = {
        this.pos = position
        this
    }
}
