package lexer

trait MyPositional {
    var pos: PyPosition = NoPosition

    def setPos(pos: PyPosition): this.type = {
        this.pos = pos
        this
    }
}
