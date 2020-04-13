package analysis

import astnodes.Program

class SemanticAnalysis {

    def process(program: Program): Program = {
        val declarationAnalyzer = new DeclarationAnalyzer()
        program.dispatch(declarationAnalyzer)

        if (program.errors.isEmpty) {
            val typeChecker = new TypeChecker()
            program.dispatch(typeChecker)
        }

        program
    }
}
