package lexer

trait MyPositional {
    var pos: PyPosition = NoPosition
    def setPos(position: PyPosition): this.type = {
        pos = position
        this
    }
}
