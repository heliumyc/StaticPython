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

    def analyze(node: Program): T = {
        node.stmtList.map(_.dispatch(this))
        defaultAction()
    }

    def analyze(node: ClassDef): T = {
        node.className.dispatch(this)
        node.baseClass.dispatch(this)
        node.declarations.map(_.dispatch(this))
        defaultAction()
    }

    def analyze(node: FuncDef): T = {
        node.funcName.dispatch(this)
        node.params.map(_.dispatch(this))
        node.returnType.map(_.dispatch(this))
        defaultAction()
    }

    def analyze(node: VarDef): T = {
        node.typedVar.dispatch(this)
        node.value.dispatch(this)
        defaultAction()
    }

    def analyze(node: AssignExpr): T = {
        node.target.dispatch(this)
        node.value.dispatch(this)
        defaultAction()
    }

    def analyze(node: BinaryExpr): T = {
        node.leftExpr.dispatch(this)
        node.rightExpr.dispatch(this)
        defaultAction()
    }

    def analyze(node: CallExpr): T = {
        node.callee.dispatch(this)
        node.args.map(_.dispatch(this))
        defaultAction()
    }

    def analyze(node: Identifier): T = {
        defaultAction()
    }

    def analyze(node: IfExpr): T = {
        node.condition.dispatch(this)
        node.thenExpr.dispatch(this)
        node.elseExpr.dispatch(this)
        defaultAction()
    }

    def analyze(node: IndexExpr): T = {
        node.callee.dispatch(this)
        node.index.dispatch(this)
        defaultAction()
    }

    def analyze(node: ListExpr): T = {
        node.vars.map(_.dispatch(this))
        defaultAction()
    }

    def analyze(node: MemberExpr): T = {
        node.callee.dispatch(this)
        node.member.dispatch(this)
        defaultAction()
    }

    def analyze(node: TupleExpr): T = {
        node.vars.map(_.dispatch(this))
        defaultAction()
    }

    def analyze(node: UnaryExpr): T = {
        node.operand.dispatch(this)
        defaultAction()
    }

    def analyze(node: BoolLiteral): T = defaultAction()

    def analyze(node: FloatLiteral): T = defaultAction()

    def analyze(node: IntegerLiteral): T = defaultAction()

    def analyze(node: NoneLiteral): T = defaultAction()

    def analyze(node: StringLiteral): T = defaultAction()

    def analyze(node: BreakStmt): T = defaultAction()

    def analyze(node: ContinueStmt): T = defaultAction()

    def analyze(node: DelStmt): T = {
        node.idList.map(_.dispatch(this))
        defaultAction()
    }

    def analyze(node: ErrorStmt): T = defaultAction()

    def analyze(node: ForStmt): T = {
        node.iterator.dispatch(this)
        node.iterable.dispatch(this)
        node.body.map(_.dispatch(this))
        defaultAction()
    }

    def analyze(node: IfStmt): T = {
        node.condition.dispatch(this)
        node.thenBody.map(_.dispatch(this))
        node.elseBody.map(_.dispatch(this))
        defaultAction()
    }

    def analyze(node: ImportStmt): T = defaultAction()

    def analyze(node: PassStmt): T = defaultAction()

    def analyze(node: ReturnStmt): T = {
        node.returnExpr.dispatch(this)
        defaultAction()
    }

    def analyze(node: TypedVar): T = {
        node.identifier.dispatch(this)
        node.varType.dispatch(this)
        defaultAction()
    }

    def analyze(node: WhileStmt): T = defaultAction()

    def analyze(vType: ClassType): T = defaultAction()

    def analyze(vType: FuncType): T = defaultAction()

    def analyze(vType: ListType): T = defaultAction()

    def analyze(vType: TupleType): T =  defaultAction()

    def analyze(v: Undefined): T = defaultAction()

    def defaultAction(): T
}
