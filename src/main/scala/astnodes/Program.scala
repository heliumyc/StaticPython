package astnodes

import common.Error

import scala.collection.mutable.ListBuffer

case class Program(stmtList: List[Statement]) extends PyAst {
    val errors: ListBuffer[Error] = ListBuffer[Error]()

    def addError(error: Error): this.type = {
        errors.append(error)
        this
    }

    def addError(errorList: Iterable[Error]): this.type = {
        errors.addAll(errorList)
        this
    }
}
