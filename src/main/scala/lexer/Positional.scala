package lexer

trait Positional {
    var line = 0
    var column = 0
    def setPos(line:Int, column: Int): this.type = {
        this.line = line
        this.column = column
        this
    }
}
