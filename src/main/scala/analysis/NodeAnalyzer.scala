package analysis

import astnodes._
import astnodes.declarations._
import astnodes.expressions._
import astnodes.literals._
import astnodes.types.{ClassType, FuncType, ListType, TupleType}
import common.PyError

import scala.collection.mutable.ListBuffer

trait NodeAnalyzer[T] {

    val errors: ListBuffer[PyError] = ListBuffer[PyError]()

    def emitError(error: PyError): this.type = {
        errors.addOne(error)
        this
    }

    def analyze(node: Program): T

    def analyze(node: ClassDef): T

    def analyze(node: FuncDef): T

    def analyze(node: VarDef): T

    def analyze(node: BlockStmt): T

    def analyze(node: AssignExpr): T

    def analyze(node: BinaryExpr): T

    def analyze(node: CallExpr): T

    def analyze(node: Identifier): T

    def analyze(node: IfExpr): T

    def analyze(node: IndexExpr): T

    def analyze(node: ListExpr): T

    def analyze(node: MemberExpr): T

    def analyze(node: TupleExpr): T

    def analyze(node: UnaryExpr): T

    def analyze(node: BoolLiteral): T

    def analyze(node: FloatLiteral): T

    def analyze(node: IntegerLiteral): T

    def analyze(node: NoneLiteral): T

    def analyze(node: StringLiteral): T

    def analyze(node: BreakStmt): T

    def analyze(node: ContinueStmt): T

    def analyze(node: DelStmt): T

    def analyze(node: ErrorStmt): T

    def analyze(node: ForStmt): T

    def analyze(node: IfStmt): T

    def analyze(node: ImportStmt): T

    def analyze(node: PassStmt): T

    def analyze(node: ReturnStmt): T

    def analyze(node: TypedVar): T

    def analyze(node: WhileStmt): T

    def analyze(v: Undefined): T
}
