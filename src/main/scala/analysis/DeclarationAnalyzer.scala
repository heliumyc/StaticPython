package analysis

import astnodes._
import astnodes.declarations.{ClassDef, Declaration, FuncDef, VarDef}
import astnodes.expressions._
import astnodes.types._
import common.{ClassInfo, PyError, SymbolTable}

import scala.collection.mutable

class DeclarationAnalyzer extends NodeAnalyzer[Unit] {

    val scopesMap: mutable.HashMap[Declaration, SymbolTable[PyType]] = mutable.HashMap()
    val globalDecls: SymbolTable[PyType] = SymbolTable.defaultGlobalSyms
    val classDecls: SymbolTable[ClassInfo] = SymbolTable.classTable
    var currentSymbols: SymbolTable[PyType] = globalDecls

    def subScope(block: => Unit): Unit = {
        // enter scope
        currentSymbols = new SymbolTable(Some(currentSymbols))
        // execute
        block
        // exit scope
        currentSymbols = currentSymbols.parent.getOrElse(globalDecls)
    }

    override def analyze(program: Program): Unit = {
        program.stmtList.foreach(_.dispatch(this))
        program.addError(this.errors)
    }

    override def analyze(node: ClassDef): Unit = {
        if (classDecls.isDeclared(node.className.name)) {
            this.emitError(PyError(s"Duplicated class declaration: '${node.className.name}'", node.pos))
        }
        // this is the scope inside of class
        subScope {
            // add symbol "self"
            currentSymbols.put("self", ClassType(node.className.name))
            node.declarations.map(_.dispatch(this))
        }
    }

    override def analyze(node: FuncDef): Unit = {
        if (currentSymbols.isDeclared(node.funcName.name)) {
            this.emitError(PyError(s"Duplicated declaration: '${node.funcName.name}'", node.pos))
        }

        // enter scope of this function
        subScope {
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
            if (node.returnType.isDefined) {
                val rt = node.returnType.get.getNestedType
                if (!classDecls.isDeclared(rt.name)) {
                    this.emitError(PyError(s"Undefined return type: '${rt.name}'", node.pos))
                }
            }
            // function body statement
            node.funcBody.map(_.dispatch(this))
        }
        None
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
        val vType = node.varType.getNestedType

        if (!classDecls.isDeclared(vType.name)) {
            this.emitError(PyError(s"Undefined variable type: '${vType.name}'", node.pos))
        }
    }

    override def analyze(node: IfStmt): Unit = {
        subScope {
            node.condition.dispatch(this)
            node.thenBody.map(_.dispatch(this))
        }
        subScope {
            node.elseBody.map(_.dispatch(this))
        }
        None
    }

    override def analyze(node: ForStmt): Unit = {
        subScope {
            // iterator must be unique in this scope
            currentSymbols.put(node.iterator.name, ClassType("Iterator"))
            node.iterable.dispatch(this)
            node.body.map(_.dispatch(this))
        }
    }

    override def analyze(node: WhileStmt): Unit = {
        subScope {
            node.condition.dispatch(this)
            node.body.map(_.dispatch(this))
        }
    }

    override def analyze(node: Identifier): Unit = {
        if (!currentSymbols.findName(node.name)) {
            // cannot find id symbol
            this.emitError(PyError(s"Symbol '${node.name}' is not found in scope", node.pos))
        }
    }

    override def defaultAction(): Unit = {}
}
