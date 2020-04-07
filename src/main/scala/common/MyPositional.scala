package common

trait MyPositional {
    var pos: Position = NoPosition
    def setPos(position: Position): this.type = {
        pos = position
        this
    }
}
