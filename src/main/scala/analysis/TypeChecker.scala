package analysis

import astnodes._
import astnodes.declarations.{ClassDef, FuncDef, VarDef}
import astnodes.expressions._
import astnodes.literals._
import astnodes.operators._
import astnodes.types.{NoneType, _}
import common.{ClassInfo, ClassUtils, Position, PyError, SymbolTable}

import scala.collection.mutable

class TypeChecker(val scopesMap: mutable.HashMap[ScopedAst, SymbolTable[PyType]],
                  val classDecls: SymbolTable[ClassInfo],
                  val globalDecls: SymbolTable[PyType]) extends NodeAnalyzer[Option[PyType]] {

    var currentSymbols: SymbolTable[PyType] = globalDecls
    val classUtils: ClassUtils = ClassUtils(classDecls)
    var currentFunc: Option[FuncType] = None

    private def emitErrorAndNone(err: PyError): Option[PyType] = {
        this.emitError(err)
        None
    }

    override def analyze(program: Program): Option[PyType] = {
        program.stmtList.foreach(_.dispatch(this))
        scopesMap(program)
        program.addError(this.errors)
        None
    }

    override def analyze(node: ClassDef): Option[PyType] = {
        currentSymbols = scopesMap(node)
        node.declarations.map(_.dispatch(this))
        None
    }

    override def analyze(node: FuncDef): Option[PyType] = {
        currentSymbols = scopesMap(node.funcBody)
        node.params.foreach(_.dispatch(this))
        val funcOption = currentSymbols.get(node.funcName.name)
        // fatal error
        assert(funcOption.isDefined)
        assert(funcOption.get.isInstanceOf[FuncType])
        currentFunc = funcOption.map(_.asInstanceOf[FuncType])
        val funcBodyType = node.funcBody.dispatch(this)
        if (funcBodyType.isEmpty && node.returnType != NoneType()) {
            this.emitError(PyError(s"all path in function body must have return path", node.funcBody.pos))
        }
        None
    }

    override def analyze(node: VarDef): Option[PyType] = {
        val rhsTypeOption = node.value match {
            case Undefined() => node.value.setInferredType(node.typedVar.varType) // no rhs, initialize a default val
            case _ => node.value.dispatch(this)
        }
        if (rhsTypeOption.isDefined && !classUtils.isSubtype(node.typedVar.varType, rhsTypeOption.get)) {
            this.emitError(PyError(s"initial value assignment type mismatch: " +
                s"required: ${node.typedVar.varType}, " +
                s"found: ${rhsTypeOption.get}", node.typedVar.pos))
        }
        None
    }

    override def analyze(node: AssignExpr): Option[PyType] = {
        (for {
            leftType <- node.target.dispatch(this)
            rightType <- node.value.dispatch(this)
        } yield {
            // assignment coercion
            // float is compatible for all int
            if (leftType == ClassType.floatType && rightType == ClassType.intType) {
                node.value.setCoersionType(ClassType.floatType)
            } else if (!classUtils.isSubtype(leftType, rightType)) {
                emitErrorAndNone(PyError(s"Assignment type is not compatible: " +
                    s"declared type $leftType found $rightType", node.value.pos))
            } else {
                node.setInferredType(leftType)
            }
        }).flatten
    }

    private def checkLeftOpRight(op: BinaryOp, leftType: PyType, rightType: PyType, position: Position): Option[PyType] = {
        // check left and right
        val opName = Operator.operatorNameMap(op)
        val leftClass: ClassInfo = leftType match {
            case ClassType(name) => classDecls.get(name).get
            case ListType(leftEle) =>
                // list has only is/+/*  three operators
                op match {
                    case Is() => return Some(ClassType.boolType)
                    case Plus() => rightType match {
                        case ListType(rightEle) =>
                            if (leftEle == NoneType())
                                return Some(rightType)
                            else if (!classUtils.isSubtype(leftEle, rightEle))
                                return emitErrorAndNone(PyError(s"list concatenation type mismatch: required: $leftType, found: $rightType", position))
                            else
                                return Some(leftType)
                    }
                    case Multiply() =>
                        if (rightType != ClassType.intType)
                            return emitErrorAndNone(PyError(s"list $op argument mismatch: required: <int>, found: $rightType", position))
                        else
                            return Some(leftType)
                    case _ => return emitErrorAndNone(PyError(s"list does not have $op operator", position))
                }
            case _: FuncType => return emitErrorAndNone(PyError("function does not have any binary operator defined", position))
            case TupleType(_) => ClassInfo.klassTuple
            case NoneType() => ClassInfo.klassNone
        }
        val opFunc = classUtils.findMethod(leftClass, opName, List(rightType))
        if (opFunc.isEmpty) {
            emitErrorAndNone(PyError(s"$leftType does not operator method $opName($rightType)", position))
        } else {
            Some(opFunc.get.returnType)
        }
    }

    override def analyze(node: BinaryExpr): Option[PyType] = {
        // x op y
        // this is the tricky part
        // for normal operator and overload, there is no coercion
        // for int and float and op is arithmetic op specifically, int is always translated into float
        // coercion operators are == < > <= >= + - * / % // **

        val leftTypeOption = node.leftExpr.dispatch(this)
        val rightTypeOption = node.rightExpr.dispatch(this)

        (for {
            leftType <- leftTypeOption
            rightType <- rightTypeOption
        } yield {
            // coercion will be treated specifically in code gen
            if (leftType == ClassType.intType && rightType == ClassType.floatType && Operator.isCoercionOp(node.op)) {
                // left is coerced to float
                node.leftExpr.setCoersionType(ClassType.floatType)
            } else if (leftType == ClassType.floatType && rightType == ClassType.intType && Operator.isCoercionOp(node.op)) {
                // right is coerced to float
                node.rightExpr.setCoersionType(ClassType.floatType)
            }
            checkLeftOpRight(node.op,
                node.leftExpr.coercionType.getOrElse(leftType),
                node.rightExpr.coercionType.getOrElse(rightType), node.pos)
        }).flatten
    }

    override def analyze(node: UnaryExpr): Option[PyType] = {
        // op x
        // check
        // 1) x has __pos__/__neg__/__bool__
        // first check if x has the method of op
        val opFuncName = Operator.operatorNameMap(node.op)
        val exprTypeOption = node.operand.dispatch(this)
        exprTypeOption.flatMap(exprType => {
            val klass: ClassInfo = exprType match {
                case ClassType(name) => classDecls.get(name).get
                case ListType(_) => ClassInfo.klassList
                case _: FuncType =>
                    this.emitError(PyError("function does not have any unary operator defined", node.pos))
                    return None
                case TupleType(_) => ClassInfo.klassTuple
                case NoneType() => ClassInfo.klassNone
            }
            val opFunc = classUtils.findMethod(klass, opFuncName, Nil)
            if (opFunc.isEmpty)
                emitErrorAndNone(PyError(s"$exprType does not unary operator method $opFuncName()", node.pos))
            else
                node.setInferredType(opFunc.get.returnType)
        })
    }

    override def analyze(node: Identifier): Option[PyType] = {
        currentSymbols.get(node.name) match {
            case Some(v) => node.setInferredType(v)
            case None => classDecls.get(node.name) match {
                case Some(c) => node.setInferredType(ClassType(c.className))
                case None => emitErrorAndNone(PyError(s"cannot find symbol ${node.name}", node.pos))
            }
        }
    }

    private def checkConditionIsValid(condType: PyType, position: Position): Unit = {
        condType match {
            case ClassType(name) =>
                val boolFuncOption = classUtils.findMethod(classDecls.get(name).get, "__bool__", Nil)
                boolFuncOption match {
                    case Some(f) =>
                        f.returnType match {
                            case ClassType.boolType =>
                            case _ => this.emitError(PyError(s"__bool__ function return type mismatch", position))
                        }
                    case None => this.emitError(PyError(s"cannot find function __bool__()=> bool", position))
                }
            case ListType(_) | TupleType(_) | NoneType() =>
        }
    }

    override def analyze(node: IfExpr): Option[PyType] = {
        val condType = node.condition.dispatch(this)
        val thenType = node.thenExpr.dispatch(this)
        val elseType = node.elseExpr.dispatch(this)
        condType match {
            case Some(t) => checkConditionIsValid(t, node.condition.pos)
            case None => return None
        }

        thenType.flatMap {
            case t1: ValueType =>
                elseType.flatMap {
                    case t2: ValueType => node.setInferredType(classUtils.findLeastCommonBaseClass(t1, t2))
                    case _ => emitErrorAndNone(PyError(s"invalid type in else block: $elseType", node.elseExpr.pos))
                }
            case _ => emitErrorAndNone(PyError(s"invalid type in then block: $thenType", node.thenExpr.pos))
        }
    }

    override def analyze(node: IndexExpr): Option[PyType] = {
        // check:
        // 1) index must be int
        // 2) must be indexed, aka list type (for now, int future might support overload)
        val listType = node.callee.dispatch(this)
        val index = node.index.dispatch(this)
        index match {
            case Some(ClassType("int")) | None =>
            case Some(x@_) => this.emitError(PyError(s"type mismatch: require int found $x", node.pos))
        }
        listType.flatMap {
            case ListType(elementType) => node.setInferredType(elementType)
            case x => emitErrorAndNone(PyError(s"type $x cannot be indexed", node.callee.pos))
        }
    }

    override def analyze(node: ListExpr): Option[PyType] = {
        // check
        // 1) infer all elements' common type
        val elementsWithNode = node.vars.map(x => (x, x.dispatch(this)))
        if (elementsWithNode.isEmpty) {
            node.setInferredType(ListType(NoneType())) // this means empty list
        } else if (elementsWithNode.forall(_._2.isDefined)) {
            val inferredType = elementsWithNode.foldLeft[ValueType](NoneType())((t1: ValueType, t2: (Expression, Option[PyType])) => {
                t2._2.get match {
                    case x: ValueType => classUtils.findLeastCommonBaseClass(t1, x)
                    case x: FuncType =>
                        this.emitError(PyError(s"invalid type in list: $x", t2._1.pos))
                        t1
                }
            })
            node.setInferredType(ListType(inferredType))
        } else None
    }

    override def analyze(node: TupleExpr): Option[PyType] = {
        // check every element type in tuple is valid
        val elements = node.vars.map(_.dispatch(this))
        if (elements.isEmpty)
            emitErrorAndNone(PyError(s"tuple cannot be empty", node.pos))
        else {
            val eleValTypes = (node.vars zip elements).map(pair => {
                if (pair._2.isEmpty) return None
                pair._2.get match {
                    case x: ValueType => x
                    case f: FuncType =>
                        return emitErrorAndNone(PyError(s"tuple element cannot be function, found $f", pair._1.pos))
                }
            })
            node.setInferredType(TupleType(eleValTypes))
        }
    }

    override def analyze(node: MemberExpr): Option[PyType] = {
        val calleeTypeOption = node.callee.dispatch(this)
        if (calleeTypeOption.isEmpty) return None
        val klass: ClassInfo = calleeTypeOption.get match {
            // if member needed is a var then return it
            // if member is a function we might need to search all func with same name
            case ClassType(klass) =>
                classDecls.get(klass) match {
                    case None =>
                        return emitErrorAndNone(PyError(s"cannot find type $klass", node.pos))
                    case Some(x) if ClassInfo.isNativeType(x.className) =>
                        return emitErrorAndNone(PyError(s"native type attributes cannot be accessed", node.pos))
                    case Some(x) => x
                }
            case _: ListType => ClassInfo.klassList
            case _: TupleType => ClassInfo.klassTuple
            case NoneType() => ClassInfo.klassNone
            case x: FuncType => return emitErrorAndNone(PyError(s"$x does not have members to access", node.pos))
        }

        val varMem = klass.getVarMember(node.member.name)
        val funcMem = klass.getFuncMember(node.member.name)
        if (varMem.isDefined) {
            node.setInferredType(varMem.get.varType)
        } else if (funcMem.isDefined) {
            node.setInferredType(funcMem.get)
        } else {
            emitErrorAndNone(PyError(s"$klass does not have member: ${node.member.name}", node.member.pos))
        }
    }

    override def analyze(node: CallExpr): Option[PyType] = {
        val calleeTypeOption = node.callee.dispatch(this)
        val argsTypeOption = node.args.map(_.dispatch(this))
        if (calleeTypeOption.isEmpty || !argsTypeOption.forall(_.isDefined)) return None
        val argsTypes = argsTypeOption.map(_.get)
        calleeTypeOption.get match {
            case f: FuncType =>
                // deal with argument coercion
                // [(node.arg, f.params, argsTypes)]
                if (node.args.length != f.params.length) {
                    emitErrorAndNone(PyError(s"arguments lens mismatch, " +
                        s"required: ${f.params.length} " +
                        s"found: ${node.args.length}", node.pos))
                } else {
                    (node.args, f.params, argsTypes).zipped.foreach { case (e, param, arg) =>
                        if (param == ClassType.floatType && arg == ClassType.intType) {
                            e.setCoersionType(ClassType.floatType)
                        } else if (!classUtils.isSubtype(param, arg)) {
                            return emitErrorAndNone(PyError(s"function type mismatch, " +
                                s"required: (${f.params.mkString(",")}), " +
                                s"found: (${argsTypes.mkString(",")})", node.pos))
                        }
                    }
                    node.setInferredType(f.returnType)
                }
            case c@ClassType(klassName) =>
                val klass = classDecls.get(klassName)
                if (klass.isDefined) {
                    if (ClassInfo.isNativeType(klassName)) {
                        return emitErrorAndNone(PyError(s"native types have no constructor", node.pos))
                    }
                    val initParams = klass.get.constructor.params
                    if (node.args.length != initParams.length) {
                        return emitErrorAndNone(PyError(s"constructor arguments lens mismatch, " +
                            s"required: ${initParams.length}, " +
                            s"found: ${node.args.length}", node.pos))
                    } else
                        (node.args, initParams, argsTypes).zipped.foreach { case (e, param, arg) =>
                            if (param == ClassType.floatType && arg == ClassType.intType) {
                                e.setCoersionType(ClassType.floatType)
                            } else if (!classUtils.isSubtype(param, arg)) {
                                return emitErrorAndNone(PyError(s"constructor args types mismatch, " +
                                    s"required: (${initParams.mkString(",")}, " +
                                    s"found: (${argsTypes.mkString(",")}", node.pos))
                            }
                        }
                    node.setInferredType(c)
                } else {
                    emitErrorAndNone(PyError(s"invalid class type $c", node.pos))
                }
            case x@_ => emitErrorAndNone(PyError(s"$x cannot be called", node.pos))
        }
    }

    override def analyze(node: ForStmt): Option[PyType] = {
        currentSymbols = scopesMap(node.body)
        val listExpr = node.iterable.dispatch(this)
        if (listExpr.isDefined) {
            listExpr.get match {
                case ListType(e) => node.iterator.setInferredType(e)
                case x@_ => this.emitError(PyError(s"type mismatch, require list type found $x", node.iterable.pos))
            }
            node.body.dispatch(this)
        }
        None
    }

    override def analyze(node: WhileStmt): Option[PyType] = {
        currentSymbols = scopesMap(node.body)
        val condExpr = node.condition.dispatch(this)
        if (condExpr.isDefined) {
            checkConditionIsValid(condExpr.get, node.condition.pos)
            node.body.dispatch(this)
        }
        None
    }

    override def analyze(node: IfStmt): Option[PyType] = {
        val condExpr = node.condition.dispatch(this)
        if (condExpr.isDefined) {
            checkConditionIsValid(condExpr.get, node.condition.pos)
            currentSymbols = scopesMap(node.thenBody)
            val thenType = node.thenBody.dispatch(this)
            currentSymbols = scopesMap(node.elseBody)
            val elseType = node.elseBody.dispatch(this)
            if (thenType.isDefined && elseType.isDefined) {
                thenType.get match {
                    case x: ValueType =>
                        elseType.get match {
                            case y: ValueType =>
                                Some(classUtils.findLeastCommonBaseClass(x, y))
                            case _: FuncType =>
                                this.emitError(PyError("return type cannot be function", node.elseBody.pos))
                                None
                        }
                    case _: FuncType =>
                        this.emitError(PyError("return type cannot be function", node.thenBody.pos))
                        None
                }
            } else None
        } else None
    }

    override def analyze(node: BlockStmt): Option[PyType] = {
        // only return statement can return type
        // which type does not matter, there must be some type passed back
        node.stmts.map(_.dispatch(this)).foldLeft[Option[PyType]](None)((acc, t) => {
            t match {
                case Some(_) => t
                case None => acc
            }
        })
    }

    override def analyze(node: BreakStmt): Option[PyType] = None

    override def analyze(node: ContinueStmt): Option[PyType] = None

    override def analyze(node: DelStmt): Option[PyType] = None

    override def analyze(node: ImportStmt): Option[PyType] = None

    override def analyze(node: PassStmt): Option[PyType] = None

    override def analyze(node: ReturnStmt): Option[PyType] = {
        val rt = node.returnExpr.dispatch(this)
        if (currentFunc.isDefined && rt.isDefined && !classUtils.isSubtype(currentFunc.get.returnType, rt.get)) {
            this.emitError(PyError(s"return expression types must be compatible with function declaration, " +
                s"required: ${currentFunc.get.returnType}, inferred: ${rt.get}", node.pos))
        }
        rt
    }

    override def analyze(node: TypedVar): Option[PyType] = {
        Some(node.varType)
    }

    override def analyze(node: BoolLiteral): Option[PyType] = {
        node.setInferredType(ClassType.boolType)
    }

    override def analyze(node: FloatLiteral): Option[PyType] = {
        node.setInferredType(ClassType.floatType)
    }

    override def analyze(node: IntegerLiteral): Option[PyType] = {
        node.setInferredType(ClassType.intType)
    }

    override def analyze(node: NoneLiteral): Option[PyType] = {
        node.setInferredType(NoneType())
    }

    override def analyze(node: StringLiteral): Option[PyType] = {
        node.setInferredType(ClassType.strType)
    }

    override def analyze(v: Undefined): Option[PyType] = {
        v.setInferredType(NoneType())
    }

    override def analyze(node: ErrorStmt): Option[PyType] = None
}
