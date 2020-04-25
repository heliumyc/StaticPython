package astnodes

import common.PyError

import scala.collection.mutable.ListBuffer

case class Program(stmtList: List[Statement]) extends PyAst with ScopedAst {
    val errors: ListBuffer[PyError] = ListBuffer[PyError]()

    def addError(error: PyError): this.type = {
        errors.append(error)
        this
    }

    def addError(errorList: Iterable[PyError]): this.type = {
        errors.addAll(errorList)
        this
    }
}
