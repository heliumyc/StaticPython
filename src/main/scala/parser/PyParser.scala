package parser

import astnodes.declarations._
import astnodes.expressions._
import astnodes.literals._
import astnodes.operators._
import astnodes.types._
import astnodes._
import common._
import lexer._
import scala.reflect.{ClassTag, classTag}

/*
    this is the first and the LAST time i ever write recursive descent parser
    hand written parser is evil, awful, dirty, meaningless and purely waste of time
 */
class PyParser(val lexer: Lexer) {
    var parseErrors: List[common.PyError] = List[common.PyError]()
    val compTokToCompOp: Map[PyToken, BinaryOp] = Map(GREATER() -> Greater(), LESS() -> Less(), GREATEREQUAL() -> GreaterEq(),
        LESSEQUAL() -> LessEq(), EQUAL() -> Equal(), IN() -> In(), IS() -> Is())
    val reader = new TokenReader(lexer)

    private def accept[T <: PyToken : ClassTag](): Either[common.PyError, T] = {
        reader.peek() match {
            case Some(x: T) =>
                reader.consume()
                Right(x)
            case Some(other) => Left(PyError(s"Expect ${classTag[T].runtimeClass.getSimpleName} but found $other at ${other.pos}", other.pos))
            case None => Left(common.PyError(s"End of file is reached", NoPosition))
        }
    }

    @scala.annotation.tailrec
    private def repeat[T, U](isInFirstSet: PyToken => Boolean, parserFunc: () => Either[PyError, T], combineOp: (T, U) => U, applyOp: U => U, acc: U): Either[PyError, U] = {
        if (reader.nonEmpty() && isInFirstSet(reader.peek().get)) {
            parserFunc() match {
                case Right(value) =>
                    repeat(isInFirstSet, parserFunc, combineOp, applyOp, combineOp(value, acc))
                case Left(err) => Left(err)
            }
        } else {
            Right(applyOp(acc))
        }
    }

    private def optional[T](isInFirstSet: PyToken => Boolean, parserFunc: () => Either[PyError, T], defaultVal: T): Either[PyError, T] = {
        if (reader.nonEmpty() && isInFirstSet(reader.peek().get)) {
            parserFunc()
        } else {
            Right(defaultVal)
        }
    }

    private def errorRecover(syncToken: PyToken, err: PyError): Unit = {
        parseErrors = err :: parseErrors
        while (reader.nonEmpty() && reader.peek().get != syncToken) {
            reader.consume()
        }
    }

    @inline
    private def eofError(expectTokName: String) = Left(common.PyError(s"Expect a $expectTokName but found EOF ", NoPosition))

    /*
        parse will return the root node of the ast
     */
    def parse(): Program = {
        var statementList = List[Statement]()
        while (reader.nonEmpty()) {
            reader.peek().get match {
                case NewLine() | EndOfFile() => reader.consume()
                case _ =>
                    statement() match {
                        case Left(err) => parseErrors = err :: parseErrors
                        case Right(s) => statementList = s :: statementList
                    }
            }
        }
        Program(statementList.reverse).addError(parseErrors.reverse)
    }

    def newlines(): Either[PyError, PyToken] = {
        accept[NewLine]() match {
            case Right(_) =>
                repeat(_==NewLine(), accept[NewLine], (x:PyToken,_:PyToken)=>x, (x:PyToken)=>x, NewLine())
            case e@Left(_) => e
        }
    }

    def statement(): Either[PyError, Statement] = {
        if (reader.nonEmpty()) {
            reader.peek().get match {
                case IF() => ifStmt()
                case WHILE() => whileStmt()
                case FOR() => forStmt()
                case DEF() => funcDef()
                case CLASS() => classDef()
                case _ => simpleStmt()
            }
        } else {
            eofError("statement")
        }
    }

    def simpleStmt(): Either[PyError, Statement] = {
        val stmt: Either[PyError, Statement] = reader.peek().get match {
            case IdentifierToken(_) =>
                // var_def or expr_stmt, here we need to lookahead two for val_decl and assign
                reader.peek(2) match {
                    case Some(COLON()) => varDef()
                    case _ => exprStmt()
                }
            case DEL() => delStmt()
            case PASS() => passStmt()
            case IMPORT() => importStmt()
            case BREAK() => breakStmt()
            case CONTINUE() => continueStmt()
            case RETURN() => returnStmt()
            case _ => exprStmt()
        }

        stmt match {
            case r@Right(_) => r
            case Left(err) =>
                errorRecover(NewLine(), err)
                Right(ErrorStmt())
        }
    }

    def varDef(): Either[PyError, VarDef] = {
        typedVarDef() match {
            case Left(err) => Left(err)
            case Right(typedVar) =>
                if (reader.nonEmpty() && reader.peek().get == EQUAL()) {
                    for {
                        _ <- accept[EQUAL]()
                        expression <- expr()
                    } yield VarDef(typedVar, expression).setPos(typedVar.pos)
                } else {
                    Right(VarDef(typedVar, Undefined()).setPos(typedVar.pos))
                }
        }
    }

    def typedVarDef(): Either[PyError, TypedVar] = {
        for {
            id <- accept[IdentifierToken]()
            _ <- accept[COLON]()
            typeName <- varType()
        } yield TypedVar(Identifier.fromToken(id), typeName).setPos(id.pos)
    }

    def varType(): Either[PyError, ValueType] = {
        // cannot be tail recursive, possible performance pitfall
        if (reader.nonEmpty()) {
            reader.peek().get match {
                case LSQB() =>
                    reader.consume()
                    for {
                        nestType <- varType()
                        _ <- accept[RSQB]()
                    } yield ListType(nestType).setPos(nestType.pos)
                case lpar@LPAR() =>
                    reader.consume()
                    for {
                        productType <- varTypeList()
                        _ <- accept[RPAR]()
                    } yield {
                        if (productType.length == 1) productType.head
                        else TupleType(productType).setPos(lpar.pos)
                    }
                case id@IdentifierToken(_) =>
                    reader.consume()
                    Right(ClassType(id.value).setPos(id.pos))
                case tok@_ => Left(common.PyError(s"Expect an identifier or list of identifier but found $tok", tok.pos))
            }
        } else {
            eofError("identifier or list of identifier")
        }
    }

    def varTypeList(): Either[PyError, List[ValueType]] = {
        varType() match {
            case Right(init) => repeat({ case IdentifierToken(_) | LSQB() | LPAR() => true; case _ => false }, () => {
                for {
                    _ <- accept[COMMA]()
                    t <- varType()
                } yield t
            }, (v: ValueType, L: List[ValueType]) => v :: L, (L: List[ValueType]) => L.reverse, init :: Nil)
            case Left(err) => Left(err)
        }
    }

    def exprStmt(): Either[PyError, Expression] = {
        val leftHandSide = test() match {
            case Right(v) => v
            case Left(err) => return Left(err)
        }

        reader.peek() match {
            case Some(EQUAL()) =>
                for {
                    _ <- accept[EQUAL]()
                    rightHandSide <- test()
                } yield AssignExpr(leftHandSide, rightHandSide).setPos(leftHandSide.pos)
            case Some(_) | None => Right(leftHandSide.setPos(leftHandSide.pos))
        }
    }

    def test(): Either[PyError, Expression] = {
        orTest() match {
            case Left(err) => Left(err)
            case Right(e) =>
                optional(_ == IF(), () => {
                    for {
                        ifTok <- accept[IF]()
                        cond <- orTest()
                        _ <- accept[ELSE]()
                        thenBody <- test()
                    } yield IfExpr(cond, e, thenBody).setPos(ifTok.pos)
                }, e)
        }
    }

    def orTest(): Either[PyError, Expression] = {
        andTest() match {
            case Left(err) => Left(err)
            case Right(init) => repeat(_ == OR(), () => {
                for {
                    _ <- accept[OR]()
                    next <- andTest()
                } yield next
            }, (e: Expression, acc: Expression) => BinaryExpr(Or(), acc, e).setPos(e.pos), (x: Expression) => x, init)
        }
    }

    def andTest(): Either[PyError, Expression] = {
        notTest() match {
            case Left(err) => Left(err)
            case Right(init) => repeat(_ == AND(), () => {
                for {
                    _ <- accept[AND]()
                    next <- notTest()
                } yield next
            }, (e: Expression, acc: Expression) => BinaryExpr(And(), acc, e).setPos(e.pos), (x: Expression) => x, init)
        }
    }

    def notTest(): Either[PyError, Expression] = {
        reader.peek() match {
            case Some(NOT()) =>
                for {
                    notTok <- accept[NOT]()
                    e <- notTest()
                } yield UnaryExpr(Not(), e).setPos(notTok.pos)
            case _ => comparison()
        }
    }

    def comparison(): Either[PyError, Expression] = {
        var leftExpr: Expression = null
        expr() match {
            case Left(err) => return Left(err)
            case Right(init) => leftExpr = init
        }

        while (reader.nonEmpty()) {
            reader.peek().get match {
                case op@(LESS() | GREATER() | LESSEQUAL() | GREATEREQUAL() | EQEQUAL() | IN()) =>
                    reader.consume()
                    expr() match {
                        case Left(err) => return Left(err)
                        case Right(e) => leftExpr = BinaryExpr(compTokToCompOp(op), leftExpr, e).setPos(e.pos)
                    }
                case op@NOTEQUAL() =>
                    reader.consume()
                    expr() match {
                        case Left(err) => return Left(err)
                        case Right(e) => leftExpr = UnaryExpr(Not(), BinaryExpr(Equal(), leftExpr, e).setPos(e.pos)).setPos(op.pos)
                    }
                case notTok@NOT() =>
                    reader.consume()
                    // not in
                    accept[IN]() match {
                        case Right(_) =>
                            expr() match {
                                case Left(err) => return Left(err)
                                case Right(e) => leftExpr = UnaryExpr(Not(), BinaryExpr(In(), leftExpr, e).setPos(e.pos)).setPos(notTok.pos)
                            }
                        case Left(err) => Left(err)
                    }
                case IS() =>
                    reader.consume()
                    reader.peek() match {
                        case Some(notTok@NOT()) =>
                            reader.consume()
                            // is not
                            expr() match {
                                case Left(err) => return Left(err)
                                case Right(e) => leftExpr = UnaryExpr(Not(), BinaryExpr(Is(), leftExpr, e).setPos(e.pos)).setPos(notTok.pos)
                            }
                        case _ =>
                            // is
                            expr() match {
                                case Left(err) => return Left(err)
                                case Right(e) => leftExpr = BinaryExpr(Is(), leftExpr, e).setPos(e.pos)
                            }
                    }
                case _ => return Right(leftExpr)
            }
        }
        Right(leftExpr)
    }

    private def combineBinaryTrailer(pair: (BinaryOp, Expression), acc: Expression) = {
        BinaryExpr(pair._1, acc, pair._2).setPos(pair._2.pos)
    }

    def expr(): Either[PyError, Expression] = {
        term() match {
            case Left(err) => Left(err)
            case Right(init) => repeat({ case PLUS() | MINUS() => true; case _ => false }, () => {
                val someOp = reader.consume().get match {
                    case PLUS() => Right(Plus())
                    case MINUS() => Right(Minus())
                    case tok@_ => Left(common.PyError("Fatal error", tok.pos))
                }
                for {
                    op <- someOp
                    next <- term()
                } yield (op, next)
            }, combineBinaryTrailer, (x: Expression) => x, init)
        }
    }

    def term(): Either[PyError, Expression] = {
        factor() match {
            case Left(err) => Left(err)
            case Right(init) => repeat({ case STAR() | SLASH() | PERCENT() | DOUBLESLASH() => true; case _ => false }, () => {
                val someOp = reader.consume().get match {
                    case STAR() => Right(Multiply())
                    case SLASH() => Right(Divide())
                    case PERCENT() => Right(Modular())
                    case DOUBLESLASH() => Right(FloorDiv())
                    case tok@_ => Left(common.PyError("Fatal error", tok.pos))
                }
                for {
                    op <- someOp
                    next <- term()
                } yield (op, next)
            }, combineBinaryTrailer, (x: Expression) => x, init)
        }
    }

    def factor(): Either[PyError, Expression] = {
        reader.peek() match {
            case Some(PLUS()) =>
                for {
                    op <- accept[PLUS]()
                    fac <- factor()
                } yield UnaryExpr(Positive(), fac).setPos(op.pos)
            case Some(MINUS()) =>
                for {
                    op <- accept[MINUS]()
                    fac <- factor()
                } yield UnaryExpr(Negative(), fac).setPos(op.pos)
            case _ =>
                for {
                    pow <- power()
                } yield pow
        }
    }

    def power(): Either[PyError, Expression] = {
        atomExpr() match {
            case Left(err) => Left(err)
            case Right(init) =>
                optional(_ == DOUBLESTAR(), () => {
                    for {
                        _ <- accept[DOUBLESTAR]()
                        fac <- factor()
                    } yield BinaryExpr(Power(), init, fac).setPos(fac.pos)
                }, init)
        }
    }

    def atomExpr(): Either[PyError, Expression] = {
        atom() match {
            case Right(initVal) =>
                repeat({ case LPAR() | LSQB() | DOT() => true; case _ => false }, trailer, (e: AtomExpression, acc: Expression) => {
                    // this might look weird, because it is right most combined.
                    e match {
                        case CallExpr(_, args) => CallExpr(acc, args).setPos(e.pos)
                        case IndexExpr(_, index) => IndexExpr(acc, index).setPos(e.pos)
                        case MemberExpr(_, member) => MemberExpr(acc, member).setPos(e.pos)
                    }
                }, (x: Expression) => x, initVal)
            case Left(err) => Left(err)
        }
    }

    def atom(): Either[PyError, Expression] = {
        def tupleExpr(): Either[PyError, Expression] = {
            accept[LPAR]() match {
                case Right(lpar) =>
                    reader.peek() match {
                        case Some(RPAR()) => reader.consume(); Right(TupleExpr(Nil).setPos(lpar.pos))
                        case Some(_) => for {
                            args <- testList()
                            _ <- accept[RPAR]()
                        } yield {
                            if (args.size > 1) TupleExpr(args).setPos(lpar.pos)
                            else if (args.size == 1) args.head   // not tuple, just normal bracket
                            else TupleExpr(Nil).setPos(lpar.pos) // this is a dummy branch which we will never reach
                        }
                        case None => eofError("list of expressions")
                    }
                case Left(err) => Left(err)
            }
        }

        def listExpr(): Either[PyError, ListExpr] = {
            accept[LSQB]() match {
                case Right(lsqb) =>
                    reader.peek() match {
                        case Some(RSQB()) => reader.consume(); Right(ListExpr(List()).setPos(lsqb.pos))
                        case Some(_) => for {
                            list <- testList()
                            _ <- accept[RSQB]()
                        } yield ListExpr(list).setPos(lsqb.pos)
                        case None => eofError("list of expressions")
                    }
                case Left(err) => Left(err)
            }
        }

        if (reader.nonEmpty()) {
            val tok = reader.peek().get
            tok match {
                case LPAR() => tupleExpr()
                case LSQB() => listExpr()
                case id@IdentifierToken(_) => reader.consume(); Right(Identifier.fromToken(id))
                case FloatPointLiteralToken(float) =>
                    reader.consume()
                    float.toDoubleOption match {
                        case Some(v) => Right(FloatLiteral(v).setPos(tok.pos))
                        case None =>
                            // parse error recovery
                            parseErrors = common.PyError("Invalid float point number", tok.pos) :: parseErrors
                            Right(FloatLiteral(0).setPos(tok.pos))
                    }
                case IntegerLiteralToken(int) =>
                    reader.consume()
                    int.toIntOption match {
                        case Some(v) => Right(IntegerLiteral(v).setPos(tok.pos))
                        case None =>
                            // parse error recovery
                            parseErrors = common.PyError("Invalid integer number", tok.pos) :: parseErrors
                            Right(IntegerLiteral(0).setPos(tok.pos))
                    }
                case StringLiteralToken(str) => reader.consume(); Right(StringLiteral(str).setPos(tok.pos));
                case NONE() => reader.consume(); Right(NoneLiteral().setPos(tok.pos))
                case TRUE() => reader.consume(); Right(BoolLiteral(true).setPos(tok.pos))
                case FALSE() => reader.consume(); Right(BoolLiteral(false).setPos(tok.pos))
                case _ => Left(common.PyError(s"Expect an tuple/list/identifier/None/True/False but found $tok", tok.pos))
            }
        } else {
            eofError("literal or list of literals")
        }
    }

    def trailer(): Either[PyError, AtomExpression] = {
        def callTrailer(): Either[PyError, CallExpr] = {
            accept[LPAR]() match {
                case Right(lpar) =>
                    reader.peek() match {
                        case Some(RPAR()) => reader.consume(); Right(CallExpr(null, Nil).setPos(lpar.pos))
                        case Some(_) => for {
                            args <- argsList()
                            _ <- accept[RPAR]()
                        } yield CallExpr(null, args).setPos(lpar.pos)
                        case None => eofError("args or close parenthesis")
                    }
                case Left(err) => Left(err)
            }
        }

        def indexTrailer(): Either[PyError, IndexExpr] = {
            for {
                lsqb <- accept[LSQB]()
                subscript <- test()
                _ <- accept[RSQB]()
            } yield IndexExpr(null, subscript).setPos(lsqb.pos)
        }

        def memberTrailer(): Either[PyError, MemberExpr] = {
            for {
                dot <- accept[DOT]()
                id <- accept[IdentifierToken]()
            } yield MemberExpr(null, Identifier.fromToken(id)).setPos(dot.pos)
        }

        reader.peek() match {
            case Some(LPAR()) => callTrailer()
            case Some(LSQB()) => indexTrailer()
            case Some(DOT()) => memberTrailer()
            case Some(tok@_) => Left(common.PyError(s"Expect a member/function/index call but found $tok", tok.pos))
            case None => eofError("member/function/index call")
        }
    }

    def argsList(): Either[PyError, List[Expression]] = testList()

    def exprList(): Either[PyError, List[Expression]] = {
        expr() match {
            case Right(init) => repeat(_ == COMMA(), () => {
                for {
                    _ <- accept[COMMA]()
                    t <- expr()
                } yield t
            }, (v: Expression, L: List[Expression]) => v :: L, (L: List[Expression]) => L.reverse, init :: Nil)
            case Left(err) => Left(err)
        }
    }

    def testList(): Either[PyError, List[Expression]] = {
        test() match {
            case Right(init) => repeat(_ == COMMA(), () => {
                for {
                    _ <- accept[COMMA]()
                    t <- test()
                } yield t
            }, (v: Expression, L: List[Expression]) => v :: L, (L: List[Expression]) => L.reverse, init :: Nil)
            case Left(err) => Left(err)
        }
    }

    def delStmt(): Either[PyError, DelStmt] = {
        def idList(): Either[PyError, List[Identifier]] = {
            accept[IdentifierToken]() match {
                case Right(init) => repeat(_ == COMMA(), () => {
                    for {
                        _ <- accept[COMMA]()
                        id <- accept[IdentifierToken]()
                    } yield id
                }, (v: IdentifierToken, L: List[Identifier]) => Identifier.fromToken(v) :: L,
                    (L: List[Identifier]) => L.reverse, Identifier.fromToken(init) :: Nil)
                case Left(err) => Left(err)
            }
        }

        for {
            op <- accept[DEL]()
            list <- idList()
        } yield DelStmt(list).setPos(op.pos)
    }

    def passStmt(): Either[PyError, PassStmt] = {
        for {
            pass <- accept[PASS]()
        } yield PassStmt().setPos(pass.pos)
    }

    def importStmt(): Either[PyError, ImportStmt] = {
        for {
            tok <- accept[IMPORT]()
            mod <- accept[IdentifierToken]()
        } yield ImportStmt(Identifier.fromToken(mod)).setPos(tok.pos)
    }

    def breakStmt(): Either[PyError, BreakStmt] = {
        for {
            tok <- accept[BREAK]()
        } yield BreakStmt().setPos(tok.pos)
    }

    def continueStmt(): Either[PyError, ContinueStmt] = {
        for {
            tok <- accept[CONTINUE]()
        } yield ContinueStmt().setPos(tok.pos)
    }

    def returnStmt(): Either[PyError, ReturnStmt] = {
        for {
            tok <- accept[RETURN]()
            list <- optional(x => x != NewLine() && x != Dedent(), exprList, Nil)
        } yield ReturnStmt(if (list.isEmpty) NoneLiteral() else if (list.size == 1) list.head else TupleExpr(list)).setPos(tok.pos)
    }

    private def testElseIfStmt(condition: Expression, thenBody: BlockStmt): Either[PyError, IfStmt] = {
        reader.peek() match {
            case Some(t@ELIF()) =>
                elifStmt() match {
                    case Right(el) => Right(IfStmt(condition, thenBody, BlockStmt(el :: Nil).setPos(el.pos)).setPos(t.pos))
                    case Left(err) => Left(err)
                }
            case Some(t@ELSE()) =>
                elseStmt() match {
                    case Right(es) => Right(IfStmt(condition, thenBody, es).setPos(t.pos))
                    case Left(err) => Left(err)
                }
            case _ => Right(IfStmt(condition, thenBody, BlockStmt(Nil).setPos(thenBody.pos)).setPos(condition.pos))
        }
    }

    def ifStmt(): Either[PyError, IfStmt] = {
        (for {
            _ <- accept[IF]()
            condition <- test()
            _ <- accept[COLON]()
            thenBody <- block()
        } yield (condition, thenBody)) match {
            case Left(err) => Left(err)
            case Right((condition, thenBody)) => testElseIfStmt(condition, thenBody)
        }
    }

    def elifStmt(): Either[PyError, IfStmt] = {
        (for {
            _ <- accept[ELIF]()
            condition <- test()
            _ <- accept[COLON]()
            thenBody <- block()
        } yield (condition, thenBody)) match {
            case Left(err) => Left(err)
            case Right((condition, thenBody)) => testElseIfStmt(condition, thenBody)
        }
    }

    def elseStmt(): Either[PyError, BlockStmt] = {
        for {
            _ <- accept[ELSE]()
            _ <- accept[COLON]()
            elseStmts <- block()
        } yield elseStmts
    }

    def whileStmt(): Either[PyError, WhileStmt] = {
        for {
            t <- accept[WHILE]()
            cond <- test()
            _ <- accept[COLON]()
            body <- block()
        } yield WhileStmt(cond, body).setPos(t.pos)
    }

    def forStmt(): Either[PyError, ForStmt] = {
        for {
            t <- accept[FOR]()
            iterator <- accept[IdentifierToken]()
            _ <- accept[IN]()
            iterable <- test()
            _ <- accept[COLON]()
            body <- block()
        } yield ForStmt(Identifier.fromToken(iterator), iterable, body).setPos(t.pos)
    }

    def block(): Either[PyError, BlockStmt] = {
        for {
            _ <- newlines()
            ind <- accept[Indent]()
            stmtList <- repeat(_ != Dedent(), () => {
                reader.peek() match {
                    case Some(IF()) => ifStmt()
                    case Some(FOR()) => forStmt()
                    case Some(WHILE()) => whileStmt()
                    case Some(linebreak@NewLine()) =>
                        reader.consume()
                        Right(PassStmt().setPos(linebreak.pos))
                    case Some(_) => simpleStmt()
                    case None => eofError("statement")
                }
            }, (s: Statement, L: List[Statement]) => {
                s match {
                    case PassStmt() => L
                    case _ => s :: L
                }
            }, (L: List[Statement]) => L.reverse, Nil)
            _ <- accept[Dedent]()
        } yield BlockStmt(stmtList).setPos(ind.pos)
    }

    def funcDef(): Either[PyError, FuncDef] = {
        for {
            t <- accept[DEF]()
            id <- accept[IdentifierToken]()
            params <- parameters()
            returnType <- optional(_ == RARROW(), () => {
                for {
                    _ <- accept[RARROW]()
                    returnType <- varType()
                } yield returnType
            }, NoneType())
            _ <- accept[COLON]()
            funcBlock <- block()
        } yield FuncDef(Identifier.fromToken(id), params, returnType, funcBlock).setPos(t.pos)
    }

    def parameters(): Either[PyError, List[TypedVar]] = {
        for {
            _ <- accept[LPAR]()
            typedArgs <- typeArgsList()
            _ <- accept[RPAR]()
        } yield typedArgs
    }

    def typeArgsList(): Either[PyError, List[TypedVar]] = {
        typedVarDef() match {
            case Right(value) =>
                repeat(_ == COMMA(),
                    () => {
                        for {
                            _ <- accept[COMMA]()
                            tpdVar <- typedVarDef()
                        } yield tpdVar
                    }, (tpdVar: TypedVar, L: List[TypedVar]) => tpdVar :: L, (L: List[TypedVar]) => L.reverse, value :: Nil)
            case Left(_) => Right(Nil)
        }
    }

    def classDef(): Either[PyError, ClassDef] = {
        val rootClassTok = IdentifierToken("object")

        def classArgsList(): Either[PyError, Identifier] = {
            reader.peek() match {
                case Some(lpar@LPAR()) =>
                    for {
                        _ <- accept[LPAR]()
                        id <- optional({ case IdentifierToken(_) => true; case _ => false },
                            accept[IdentifierToken], rootClassTok.setPos(lpar.pos))
                        _ <- accept[RPAR]()
                    } yield Identifier.fromToken(id)
                case _ => Right(Identifier.fromToken(rootClassTok))
            }
        }

        def classBody(): Either[PyError, List[Declaration]] = {
            repeat(_!=Dedent(), () => {
                reader.peek() match {
                    case Some(IdentifierToken(_)) => varDef()
                    case Some(DEF()) => funcDef()
                    case Some(PASS()) =>
                        reader.consume()
                        Right(PassStmt())
                    case Some(NewLine()) =>
                        reader.consume()
                        Right(PassStmt())
                    case Some(tok@_) => Left(common.PyError(s"Expect a declaration but found $tok", tok.pos))
                    case None => eofError("statement")
                }
            }, (t: Declaration, L: List[Declaration]) => {
                if (t == PassStmt()) L else t :: L
            }, (L: List[Declaration]) => L.reverse, Nil)
        }

        for {
            _ <- accept[CLASS]()
            className <- accept[IdentifierToken]()
            baseClass <- classArgsList()
            _ <- accept[COLON]()
            _ <- newlines()
            _ <- accept[Indent]()
            body <- classBody()
            _ <- accept[Dedent]()
        } yield ClassDef(Identifier.fromToken(className), baseClass, body)
    }
}
