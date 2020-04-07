package astnodes

import java.lang.reflect.Member

import analysis.NodeAnalyzer
import astnodes.declarations.{ClassDef, FuncDef, VarDef}
import astnodes.expressions.{AssignExpr, BinaryExpr, CallExpr, Identifier, IfExpr, IndexExpr, ListExpr, MemberExpr, TupleExpr, UnaryExpr}
import astnodes.literals.{BoolLiteral, FloatLiteral, IntegerLiteral, NoneLiteral, StringLiteral}
import common.{NoPosition, Position}

trait PyAst {
    var pos:Position = NoPosition
    def setPos(position: Position): this.type = {
        this.pos = position
        this
    }

    def dispatch[T](nodeAnalyzer: NodeAnalyzer[T]): T = {
        this match {
            case t:ClassDef => nodeAnalyzer.analyze(t)
            case t:FuncDef => nodeAnalyzer.analyze(t)
            case t:VarDef => nodeAnalyzer.analyze(t)
            case t:AssignExpr => nodeAnalyzer.analyze(t)
            case t:BinaryExpr => nodeAnalyzer.analyze(t)
            case t:CallExpr => nodeAnalyzer.analyze(t)
            case t:Identifier => nodeAnalyzer.analyze(t)
            case t:IfExpr => nodeAnalyzer.analyze(t)
            case t:IndexExpr => nodeAnalyzer.analyze(t)
            case t:ListExpr => nodeAnalyzer.analyze(t)
            case t:MemberExpr => nodeAnalyzer.analyze(t)
            case t:TupleExpr => nodeAnalyzer.analyze(t)
            case t:UnaryExpr => nodeAnalyzer.analyze(t)
            case t:BoolLiteral => nodeAnalyzer.analyze(t)
            case t:FloatLiteral => nodeAnalyzer.analyze(t)
            case t:IntegerLiteral => nodeAnalyzer.analyze(t)
            case t:NoneLiteral => nodeAnalyzer.analyze(t)
            case t:StringLiteral => nodeAnalyzer.analyze(t)
            case t:BreakStmt => nodeAnalyzer.analyze(t)
            case t:ContinueStmt => nodeAnalyzer.analyze(t)
            case t:DelStmt => nodeAnalyzer.analyze(t)
            case t:ErrorStmt => nodeAnalyzer.analyze(t)
            case t:ForStmt => nodeAnalyzer.analyze(t)
            case t:IdType => nodeAnalyzer.analyze(t)
            case t:IfStmt => nodeAnalyzer.analyze(t)
            case t:ImportStmt => nodeAnalyzer.analyze(t)
            case t:PassStmt => nodeAnalyzer.analyze(t)
            case t:Program => nodeAnalyzer.analyze(t)
            case t:ReturnStmt => nodeAnalyzer.analyze(t)
            case t:TypedVar => nodeAnalyzer.analyze(t)
            case t:WhileStmt => nodeAnalyzer.analyze(t)
        }
    }
}
