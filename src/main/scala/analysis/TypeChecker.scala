package analysis

import astnodes.types.PyType

class TypeChecker() extends NodeAnalyzer[Option[PyType]] {
    override def defaultAction(): Option[PyType] = None
}
