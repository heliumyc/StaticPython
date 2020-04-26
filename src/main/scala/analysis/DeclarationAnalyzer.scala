package analysis

import astnodes._
import astnodes.declarations.{ClassDef, Declaration, FuncDef, VarDef}
import astnodes.expressions._
import astnodes.literals.{BoolLiteral, FloatLiteral, IntegerLiteral, NoneLiteral, StringLiteral}
import astnodes.types._
import common.{ClassInfo, PyError, SymbolTable}

import scala.collection.mutable

class DeclarationAnalyzer extends NodeAnalyzer[Unit] {

    val scopesMap: mutable.HashMap[ScopedAst, SymbolTable[PyType]] = mutable.HashMap()
    val globalDecls: SymbolTable[PyType] = SymbolTable.getDefaultGlobalSyms
    val classDecls: SymbolTable[ClassInfo] = SymbolTable.getDefaultClassTable
    var currentSymbols: SymbolTable[PyType] = globalDecls
    var isOutsideFunc: Boolean = true

    def subScope(block: => Unit): Unit = {
        // enter scope
        currentSymbols = SymbolTable(currentSymbols)
        // execute
        block
        // exit scope
        currentSymbols = currentSymbols.parent.getOrElse(globalDecls)
    }

    override def analyze(program: Program): Unit = {
        scopesMap.put(program, globalDecls)
        program.stmtList.foreach(_.dispatch(this))
        program.addError(this.errors)
    }

    private def isProtectedClass(name: String):Boolean = {
        name == "list" || name == "str" || name == "int"|| name == "float" || name == "tuple" || name == "None" || name == "type"
    }

    override def analyze(node: ClassDef): Unit = {
        if (classDecls.isDeclared(node.className.name)) {
            this.emitError(PyError(s"Duplicated class declaration: '${node.className.name}'", node.pos))
        } else if (!classDecls.isDeclared(node.baseClass.name)) {
            this.emitError(PyError(s"Base class is not defined: '${node.baseClass.name}'", node.pos))
        } else if (globalDecls.isDeclared(node.className.name)) {
            this.emitError(PyError(s"Class name conflicts with ${globalDecls.get(node.className.name)}", node.className.pos))
        } else if (isProtectedClass(node.baseClass.name)) {
            this.emitError(PyError(s"Base class cannot be ${node.baseClass.name}", node.pos))
        } else {
            classDecls.put(node.className.name,
                ClassInfo(node.className.name, classDecls.get(node.baseClass.name),
                    node.declarations.collect {
                        case VarDef(tpfVar, _) => tpfVar
                    },
                    node.declarations.collect {
                        case f@FuncDef(funcName, params, returnType, _) =>
                            if (funcName.name == "__init__" && returnType != NoneType()) {
                                this.emitError(PyError(s"Class constructor's should not have any return type", f.pos))
                            }
                            FuncType(funcName.name, params.map(_.varType), returnType)
                    }
                )
            )

            globalDecls.put(node.className.name, ClassType(node.className.name))

            // this is the scope inside of class
            subScope {
                scopesMap.put(node, currentSymbols)
                // add symbol "self"
                currentSymbols.put("self", ClassType(node.className.name))
                node.declarations.foreach(_.dispatch(this))
            }
        }
    }

    override def analyze(node: FuncDef): Unit = {
        if (currentSymbols.isDeclared(node.funcName.name)) {
            this.emitError(PyError(s"Duplicated declaration: '${node.funcName.name}'", node.pos))
        } else if (classDecls.isDeclared(node.funcName.name)) {
            this.emitError(PyError(s"Function declaration shadows class: ${classDecls.get(node.funcName.name).get}", node.pos))
        } else {
            currentSymbols.put(node.funcName.name, FuncType(
                node.funcName.name,
                node.params.map(_.varType),
                node.returnType
            ))
        }

        // enter scope of this function
        subScope {
            scopesMap.put(node.funcBody, currentSymbols)
            // params of function
            // add them into current scope
            node.params.foreach(tpfVar => {
                val name = tpfVar.identifier.name
                if (currentSymbols.isDeclared(name)) {
                    this.emitError(PyError(s"Duplicated function parameter name: '$name'", node.pos))
                } else {
                    // add this var into symbol table
                    currentSymbols.put(name, tpfVar.varType)
                }
            })
            // check type var type definition
            node.params.foreach(_.dispatch(this))

            // return type
            if (!checkDeclType(node.returnType)) {
                this.emitError(PyError(s"Undefined return type: '${node.returnType}'", node.pos))
            }

            // function body statement
            isOutsideFunc = false
            node.funcBody.stmts.foreach(_.dispatch(this))
            isOutsideFunc = true
        }
    }

    private def checkDeclType(t: ValueType): Boolean = {
        t match {
            case ListType(elementType) => checkDeclType(elementType)
            case NoneType() => true
            case ClassType(name) => classDecls.isDeclared(name)
            case TupleType(productType) => productType.forall(checkDeclType)
        }
    }

    override def analyze(node: VarDef): Unit = {
        // check if var def is duplicated
        val name = node.typedVar.identifier.name
        if (currentSymbols.isDeclared(name)) {
            this.emitError(PyError(s"Duplicated variable declaration: '$name'", node.pos))
        } else {
            // add this var into symbol table
            currentSymbols.put(name, node.typedVar.varType)
        }

        node.typedVar.dispatch(this)
        // check if symbols in expression is defined
        node.value.dispatch(this)
    }

    override def analyze(node: TypedVar): Unit = {
        // check whether var type is defined

        if (!checkDeclType(node.varType)) {
            this.emitError(PyError(s"Undefined class type: '${node.varType}'", node.pos))
        }
    }

    override def analyze(node: IfStmt): Unit = {
        node.condition.dispatch(this)
        subScope {
            scopesMap.put(node.thenBody, currentSymbols)
            node.thenBody.stmts.foreach(_.dispatch(this))
        }
        subScope {
            scopesMap.put(node.elseBody, currentSymbols)
            node.elseBody.stmts.foreach(_.dispatch(this))
        }
    }

    override def analyze(node: ForStmt): Unit = {
        subScope {
            // iterator must be unique in this scope
            scopesMap.put(node.body, currentSymbols)
            currentSymbols.put(node.iterator.name, ClassType("int"))
            node.iterable.dispatch(this)
            node.body.stmts.map(_.dispatch(this))
        }
    }

    override def analyze(node: WhileStmt): Unit = {
        subScope {
            scopesMap.put(node.body, currentSymbols)
            node.condition.dispatch(this)
            node.body.stmts.map(_.dispatch(this))
        }
    }

    override def analyze(node: MemberExpr): Unit = {
        // we cut off propagation to callee args
        // it is left to type checker
        node.callee.dispatch(this)
    }

    override def analyze(node: Identifier): Unit = {
        if (!currentSymbols.findName(node.name) && !classDecls.isDeclared(node.name)) {
            // cannot find id symbol
            this.emitError(PyError(s"Symbol '${node.name}' is not found in scope", node.pos))
        }
    }

    override def analyze(node: BreakStmt): Unit = {
        if (currentSymbols.parent.isEmpty) {
            this.emitError(PyError(s"Invalid break in outermost scope", node.pos))
        }
    }

    override def analyze(node: ContinueStmt): Unit = {
        if (currentSymbols.parent.isEmpty) {
            this.emitError(PyError(s"Invalid continue in outermost scope", node.pos))
        }
    }

    override def analyze(node: ReturnStmt): Unit = {
        if (isOutsideFunc) {
            this.emitError(PyError(s"Invalid return found outside of function", node.pos))
        }
    }

    override def analyze(node: BlockStmt): Unit = {
        node.stmts.foreach(_.dispatch(this))
    }

    override def analyze(node: AssignExpr): Unit = {
        if (!node.target.isInstanceOf[Assignable]) {
            // assigment left hand side must be atomExpr otherwise invalid
            this.emitError(PyError(s"invalid assignment: cannot assign to left hand side", node.pos))
        }
        node.value.dispatch(this)
        node.target.dispatch(this)
    }

    override def analyze(node: BinaryExpr): Unit = {
        node.leftExpr.dispatch(this)
        node.rightExpr.dispatch(this)
    }

    override def analyze(node: CallExpr): Unit = {
        node.callee.dispatch(this)
        node.args.foreach(_.dispatch(this))
    }

    override def analyze(node: IfExpr): Unit = {
        node.condition.dispatch(this)
        node.thenExpr.dispatch(this)
        node.elseExpr.dispatch(this)
    }

    override def analyze(node: IndexExpr): Unit = {
        node.callee.dispatch(this)
        node.index.dispatch(this)
    }

    override def analyze(node: ListExpr): Unit = {
        node.vars.foreach(_.dispatch(this))
    }

    override def analyze(node: TupleExpr): Unit = {
        node.vars.foreach(_.dispatch(this))
    }

    override def analyze(node: UnaryExpr): Unit = {
        node.operand.dispatch(this)
    }

    override def analyze(node: BoolLiteral): Unit = {}

    override def analyze(node: FloatLiteral): Unit = {}

    override def analyze(node: IntegerLiteral): Unit = {}

    override def analyze(node: NoneLiteral): Unit = {}

    override def analyze(node: StringLiteral): Unit = {}

    override def analyze(node: DelStmt): Unit = {
        node.idList.foreach(_.dispatch(this))
    }

    override def analyze(node: ErrorStmt): Unit = {}

    override def analyze(node: ImportStmt): Unit = {}

    override def analyze(node: PassStmt): Unit = {}

    override def analyze(v: Undefined): Unit = {}
}
