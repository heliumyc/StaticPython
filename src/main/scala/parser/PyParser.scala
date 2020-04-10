package parser

import astnodes.declarations._
import astnodes.expressions._
import astnodes.literals._
import astnodes.operators._
import astnodes.types._
import astnodes._
import common.{AND, BREAK, CLASS, COLON, COMMA, CONTINUE, DEF, DEL, DOT, DOUBLESLASH, DOUBLESTAR, Dedent, ELIF, ELSE, EQUAL, EndOfFile, Error, FALSE, FOR, FloatPointLiteralToken, GREATER, GREATEREQUAL, IF, IMPORT, IN, IS, IdentifierToken, Indent, IntegerLiteralToken, LESS, LESSEQUAL, LPAR, LSQB, MINUS, NONE, NOT, NOTEQUAL, NewLine, NoPosition, OR, PASS, PERCENT, PLUS, Position, PyToken, RARROW, RETURN, RPAR, RSQB, SLASH, STAR, StringLiteralToken, TRUE, WHILE}
import lexer._
import scala.reflect.{ClassTag, classTag}

/*
    this is the first and the LAST time i ever write recursive descent parser
    hand written parser is evil, awful, dirty, meaningless and purely waste of time
 */
class PyParser(val lexer: Lexer) {
    var parseErrors: List[common.Error] = List[common.Error]()
    val compTokToCompOp: Map[PyToken, BinaryOp] = Map(GREATER() -> Greater(), LESS() -> Less(), GREATEREQUAL() -> GreaterEq(),
        LESSEQUAL() -> LessEq(), EQUAL() -> Equal(), IN() -> In(), IS() -> Is())
    val reader = new TokenReader(lexer)

    private def accept[T <: PyToken : ClassTag](): Either[common.Error, T] = {
        reader.peek() match {
            case Some(x: T) =>
                reader.consume()
                Right(x)
            case Some(other) => Left(Error(s"Expect ${classTag[T].runtimeClass.getSimpleName} but found $other at ${other.pos}", other.pos))
            case None => Left(common.Error(s"End of file is reached", NoPosition))
        }
    }

    @scala.annotation.tailrec
    private def repeat[T, U](isInFirstSet: PyToken => Boolean, parserFunc: () => Either[Error, T], combineOp: (T, U) => U, applyOp: U => U, acc: U): Either[Error, U] = {
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

    private def optional[T](isInFirstSet: PyToken => Boolean, parserFunc: () => Either[Error, T], defaultVal: T): Either[Error, T] = {
        if (reader.nonEmpty() && isInFirstSet(reader.peek().get)) {
            parserFunc()
        } else {
            Right(defaultVal)
        }
    }

    private def errorRecover(syncToken: PyToken, err: Error): Unit = {
        parseErrors = err :: parseErrors
        while (reader.nonEmpty() && reader.peek().get != syncToken) {
            reader.consume()
        }
    }

    @inline
    private def eofError(expectTokName: String) = Left(common.Error(s"Expect a $expectTokName but found EOF ", NoPosition))

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
        Program(statementList.reverse, parseErrors.reverse)
    }

    def statement(): Either[Error, Statement] = {
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

    def simpleStmt(): Either[Error, Statement] = {
        val stmt: Either[Error, Statement] = reader.peek().get match {
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
            case tok@_ => Left(common.Error(s"Expect a statement start but found $tok", tok.pos))
        }

        stmt match {
            case r@Right(_) => r
            case Left(err) =>
                errorRecover(NewLine(), err)
                Right(ErrorStmt())
        }
    }

    def varDef(): Either[Error, VarDef] = {
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

    def typedVarDef(): Either[Error, TypedVar] = {
        for {
            identifier <- accept[IdentifierToken]()
            _ <- accept[COLON]()
            typeName <- varType()
        } yield TypedVar(identifier, typeName).setPos(identifier.pos)
    }

    def varType(): Either[Error, PyType] = {
        // cannot be tail recursive, possible performance pitfall
        if (reader.nonEmpty()) {
            reader.peek().get match {
                case LSQB() =>
                    reader.consume()
                    for {
                        nestType <- varType()
                        _ <- accept[RSQB]()
                    } yield ListType(nestType).setPos(nestType.pos)
                case id@IdentifierToken(_) =>
                    reader.consume()
                    Right(IdType(id).setPos(id.pos))
                case tok@_ => Left(common.Error(s"Expect an identifier or list of identifier but found $tok", tok.pos))
            }
        } else {
            eofError("identifier or list of identifier")
        }
    }

    def exprStmt(): Either[Error, Expression] = {
        val leftHandSide = atomExpr() match {
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

    def test(): Either[Error, Expression] = {
        orTest() match {
            case Left(err) => Left(err)
            case Right(e) =>
                optional(_==IF(), ()=>{
                    for {
                        ifTok <- accept[IF]()
                        cond <- orTest()
                        _ <- accept[ELSE]()
                        thenBody <- test()
                    } yield IfExpr(cond, e, thenBody).setPos(ifTok.pos)
                }, e)
        }
    }

    def orTest(): Either[Error, Expression] = {
        andTest() match {
            case Left(err) => Left(err)
            case Right(init) => repeat(_==OR(), () => {
                for {
                    _ <- accept[OR]()
                    next <- andTest()
                } yield next
            }, (e: Expression, acc: Expression) => BinaryExpr(Or(), acc, e).setPos(e.pos), (x: Expression) => x, init)
        }
    }

    def andTest(): Either[Error, Expression] = {
        notTest() match {
            case Left(err) => Left(err)
            case Right(init) => repeat(_==AND(), () => {
                for {
                    _ <- accept[AND]()
                    next <- notTest()
                } yield next
            }, (e: Expression, acc: Expression) => BinaryExpr(And(), acc, e).setPos(e.pos), (x: Expression) => x, init)
        }
    }

    def notTest(): Either[Error, Expression] = {
        reader.peek() match {
            case Some(NOT()) =>
                for {
                    notTok <- accept[NOT]()
                    e <- notTest()
                } yield UnaryExpr(Not(), e).setPos(notTok.pos)
            case _ => comparison()
        }
    }

    def comparison(): Either[Error, Expression] = {
        var leftExpr: Expression = null
        expr() match {
            case Left(err) => return Left(err)
            case Right(init) => leftExpr = init
        }

        while (reader.nonEmpty()) {
            reader.peek().get match {
                case op@(LESS() | GREATER() | LESSEQUAL() | GREATEREQUAL() | EQUAL() | IN() | NOTEQUAL()) =>
                    reader.consume()
                    expr() match {
                        case Left(err) => return Left(err)
                        case Right(e) => leftExpr = BinaryExpr(compTokToCompOp(op), leftExpr, e).setPos(e.pos)
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

    def expr(): Either[Error, Expression] = {
        term() match {
            case Left(err) => Left(err)
            case Right(init) => repeat({ case PLUS() | MINUS() => true; case _ => false }, () => {
                val someOp = reader.consume().get match {
                    case PLUS() => Right(Plus())
                    case MINUS() => Right(Minus())
                    case tok@_ => Left(common.Error("Fatal error", tok.pos))
                }
                for {
                    op <- someOp
                    next <- term()
                } yield (op, next)
            }, combineBinaryTrailer, (x: Expression) => x, init)
        }
    }

    def term(): Either[Error, Expression] = {
        factor() match {
            case Left(err) => Left(err)
            case Right(init) => repeat({ case STAR() | SLASH() | PERCENT() | DOUBLESLASH() => true; case _ => false }, () => {
                val someOp = reader.consume().get match {
                    case STAR() => Right(Multiply())
                    case SLASH() => Right(Divide())
                    case PERCENT() => Right(Modular())
                    case DOUBLESLASH() => Right(FloorDiv())
                    case tok@_ => Left(common.Error("Fatal error", tok.pos))
                }
                for {
                    op <- someOp
                    next <- term()
                } yield (op, next)
            }, combineBinaryTrailer, (x: Expression) => x, init)
        }
    }

    def factor(): Either[Error, Expression] = {
        reader.peek() match {
            case Some(PLUS()) =>
                for {
                    op <- accept[PLUS]()
                    fac <- factor()
                } yield UnaryExpr(Plus(), fac).setPos(op.pos)
            case Some(MINUS()) =>
                for {
                    op <- accept[MINUS]()
                    fac <- factor()
                } yield UnaryExpr(Minus(), fac).setPos(op.pos)
            case _ =>
                for {
                    pow <- power()
                } yield pow
        }
    }

    def power(): Either[Error, Expression] = {
        atomExpr() match {
            case Left(err) => Left(err)
            case Right(init) =>
                optional(_==DOUBLESTAR(), ()=>{
                    for {
                        _ <- accept[DOUBLESTAR]()
                        fac <- factor()
                    } yield BinaryExpr(Power(), init, fac).setPos(fac.pos)
                }, init)
        }
    }

    def atomExpr(): Either[Error, Expression] = {
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

    def atom(): Either[Error, Expression] = {
        def tupleExpr(): Either[Error, TupleExpr] = {
            for {
                lpar <- accept[LPAR]()
                args <- testList()
            } yield TupleExpr(args).setPos(lpar.pos)
        }

        def listExpr(): Either[Error, ListExpr] = {
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
                case IdentifierToken(id) => reader.consume(); Right(Identifier(id).setPos(tok.pos))
                case FloatPointLiteralToken(float) =>
                    reader.consume()
                    float.toDoubleOption match {
                        case Some(v) => Right(FloatLiteral(v).setPos(tok.pos))
                        case None =>
                            // parse error recovery
                            parseErrors = common.Error("Invalid float point number", tok.pos) :: parseErrors
                            Right(FloatLiteral(0).setPos(tok.pos))
                    }
                case IntegerLiteralToken(int) =>
                    reader.consume()
                    int.toIntOption match {
                        case Some(v) => Right(IntegerLiteral(v).setPos(tok.pos))
                        case None =>
                            // parse error recovery
                            parseErrors = common.Error("Invalid integer number", tok.pos) :: parseErrors
                            Right(IntegerLiteral(0).setPos(tok.pos))
                    }
                case StringLiteralToken(str) => reader.consume(); Right(StringLiteral(str).setPos(tok.pos));
                case NONE() => reader.consume(); Right(NoneLiteral().setPos(tok.pos))
                case TRUE() => reader.consume(); Right(BoolLiteral(true).setPos(tok.pos))
                case FALSE() => reader.consume(); Right(BoolLiteral(false).setPos(tok.pos))
                case _ => Left(common.Error(s"Expect an tuple/list/identifier/None/True/False but found $tok", tok.pos))
            }
        } else {
            eofError("literal or list of literals")
        }
    }

    def trailer(): Either[Error, AtomExpression] = {
        def callTrailer(): Either[Error, CallExpr] = {
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

        def indexTrailer(): Either[Error, IndexExpr] = {
            for {
                lsqb <- accept[LSQB]()
                subscript <- test()
                _ <- accept[RSQB]()
            } yield IndexExpr(null, subscript).setPos(lsqb.pos)
        }

        def memberTrailer(): Either[Error, MemberExpr] = {
            for {
                dot <- accept[DOT]()
                id <- accept[IdentifierToken]()
            } yield MemberExpr(null, id).setPos(dot.pos)
        }

        reader.peek() match {
            case Some(LPAR()) => callTrailer()
            case Some(LSQB()) => indexTrailer()
            case Some(DOT()) => memberTrailer()
            case Some(tok@_) => Left(common.Error(s"Expect a member/function/index call but found $tok", tok.pos))
            case None => eofError("member/function/index call")
        }
    }

    def argsList(): Either[Error, List[Expression]] = testList()

    def exprList(): Either[Error, List[Expression]] = {
        expr() match {
            case Right(init) => repeat(_==COMMA(), () => {
                for {
                    _ <- accept[COMMA]()
                    t <- expr()
                } yield t
            }, (v: Expression, L: List[Expression]) => v :: L, (L: List[Expression]) => L.reverse, init :: Nil)
            case Left(err) => Left(err)
        }
    }

    def testList(): Either[Error, List[Expression]] = {
        test() match {
            case Right(init) => repeat(_==COMMA(), () => {
                for {
                    _ <- accept[COMMA]()
                    t <- test()
                } yield t
            }, (v: Expression, L: List[Expression]) => v :: L, (L: List[Expression]) => L.reverse, init :: Nil)
            case Left(err) => Left(err)
        }
    }

    def delStmt(): Either[Error, DelStmt] = {
        for {
            op <- accept[DEL]()
            id <- accept[IdentifierToken]()
        } yield DelStmt(id).setPos(op.pos)
    }

    def passStmt(): Either[Error, PassStmt] = {
        for {
            pass <- accept[PASS]()
        } yield PassStmt().setPos(pass.pos)
    }

    def importStmt(): Either[Error, ImportStmt] = {
        for {
            tok <- accept[IMPORT]()
            mod <- accept[IdentifierToken]()
        } yield ImportStmt(mod).setPos(tok.pos)
    }

    def breakStmt(): Either[Error, BreakStmt] = {
        for {
            tok <- accept[BREAK]()
        } yield BreakStmt().setPos(tok.pos)
    }

    def continueStmt(): Either[Error, ContinueStmt] = {
        for {
            tok <- accept[CONTINUE]()
        } yield ContinueStmt().setPos(tok.pos)
    }

    def returnStmt(): Either[Error, ReturnStmt] = {
        for {
            tok <- accept[RETURN]()
            list <- optional(_!=NewLine(), exprList, Nil)
        } yield ReturnStmt(list).setPos(tok.pos)
    }

    private def testElseIfStmt(condition: Expression, thenBody: List[Statement], pos:Position): Either[Error, IfStmt] = {
        reader.peek() match {
            case Some(t@ELIF()) =>
                elifStmt() match {
                    case Right(el) => Right(IfStmt(condition, thenBody, el :: Nil).setPos(t.pos))
                    case Left(err) => Left(err)
                }
            case Some(t@ELSE()) =>
                elseStmt() match {
                    case Right(es) => Right(IfStmt(condition, thenBody, es).setPos(t.pos))
                    case Left(err) => Left(err)
                }
            case _ => Right(IfStmt(condition, thenBody, Nil).setPos(condition.pos))
        }
    }

    def ifStmt(): Either[Error, IfStmt] = {
        (for {
            ifTok <- accept[IF]()
            condition <- test()
            _ <- accept[COLON]()
            thenBody <- block()
        } yield (ifTok, condition, thenBody)) match {
            case Left(err) => Left(err)
            case Right((ifTok, condition, thenBody)) => testElseIfStmt(condition, thenBody, ifTok.pos)
        }
    }

    def elifStmt(): Either[Error, IfStmt] = {
        (for {
            elifTok <- accept[ELIF]()
            condition <- test()
            _ <- accept[COLON]()
            thenBody <- block()
        } yield (elifTok, condition, thenBody)) match {
            case Left(err) => Left(err)
            case Right((elifTok, condition, thenBody)) => testElseIfStmt(condition, thenBody, elifTok.pos)
        }
    }

    def elseStmt(): Either[Error, List[Statement]] = {
        for {
            _ <- accept[ELSE]()
            _ <- accept[COLON]()
            elseStmts <- block()
        } yield elseStmts
    }

    def whileStmt(): Either[Error, WhileStmt] = {
        for {
            t <- accept[WHILE]()
            cond <- test()
            _ <- accept[COLON]()
            body <- block()
        } yield WhileStmt(cond, body).setPos(t.pos)
    }

    def forStmt(): Either[Error, ForStmt] = {
        for {
            t <- accept[FOR]()
            iterator <- accept[IdentifierToken]()
            _ <- accept[IN]()
            iterable <- test()
            _ <- accept[COLON]()
            body <- block()
        } yield ForStmt(Identifier(iterator.value), iterable, body).setPos(t.pos)
    }

    def block(): Either[Error, List[Statement]] = {
        for {
            _ <- accept[NewLine]()
            _ <- accept[Indent]()
            stmtList <- repeat(_!=Dedent(), () => {
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
        } yield stmtList
    }

    def funcDef(): Either[Error, FuncDef] = {
        for {
            t <- accept[DEF]()
            id <- accept[IdentifierToken]()
            params <- parameters()
            returnType <- optional(_==RARROW(), ()=>{
                for {
                    _ <- accept[RARROW]()
                    returnType <- varType()
                } yield Some(returnType)
            }, None)
            _ <- accept[COLON]()
            funcBlock <- block()
        } yield FuncDef(id, params, returnType, funcBlock).setPos(t.pos)
    }

    def parameters(): Either[Error, List[TypedVar]] = {
        for {
            _ <- accept[LPAR]()
            typedArgs <- typeArgsList()
            _ <- accept[RPAR]()
        } yield typedArgs
    }

    def typeArgsList(): Either[Error, List[TypedVar]] = {
        typedVarDef() match {
            case Right(value) =>
                repeat(_==COMMA(),
                    () => {
                        for {
                            _ <- accept[COMMA]()
                            tpdVar <- typedVarDef()
                        } yield tpdVar
                    }, (tpdVar: TypedVar, L: List[TypedVar]) => tpdVar :: L, (L: List[TypedVar]) => L.reverse, value :: Nil)
            case Left(_) => Right(Nil)
        }
    }

    def classDef(): Either[Error, ClassDef] = {
        val rootClassTok = IdentifierToken("Object")

        def classArgsList(): Either[Error, Identifier] = {
            reader.peek() match {
                case Some(lpar@LPAR()) =>
                    for {
                        _ <- accept[LPAR]()
                        id <- optional({case IdentifierToken(_) => true; case _=>false}, accept[IdentifierToken], rootClassTok)
                        _ <- accept[RPAR]()
                    } yield Identifier(id.value).setPos(lpar.pos)
                case _ => Right(Identifier(rootClassTok.value))
            }
        }

        def classBody(): Either[Error, List[Declaration]] = {
            repeat({
                case IdentifierToken(_) | DEF() | NewLine() => true
                case _ => false
            }, () => {
                reader.peek() match {
                    case Some(IdentifierToken(_)) => varDef()
                    case Some(DEF()) => funcDef()
                    case Some(line@NewLine()) =>
                        reader.consume()
                        Right(PassStmt().setPos(line.pos))
                    case Some(tok@_) => Left(common.Error(s"Expect a declaration but found $tok", tok.pos))
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
            _ <- accept[NewLine]()
            _ <- accept[Indent]()
            body <- classBody()
            _ <- accept[Dedent]()
        } yield ClassDef(Identifier(className.value).setPos(className.pos), baseClass, body)
    }
}
