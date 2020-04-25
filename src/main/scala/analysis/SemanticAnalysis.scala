package analysis

import astnodes.expressions.{AssignExpr, Identifier}
import astnodes.{BlockStmt, ForStmt, Program}

class SemanticAnalysis {

    def process(program: Program): Program = {
        val declarationAnalyzer = new DeclarationAnalyzer()
        program.dispatch(declarationAnalyzer)

        if (program.errors.isEmpty) {
            val typeChecker = new TypeChecker(declarationAnalyzer.scopesMap,
                declarationAnalyzer.classDecls, declarationAnalyzer.globalDecls)
            program.dispatch(typeChecker)
        }
        program
    }
}
