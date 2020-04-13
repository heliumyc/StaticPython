package astnodes.types

import scala.annotation.tailrec

trait ValueType extends PyType {

    @tailrec
    final def getNestedType: ClassType = {
        this match {
            case ListType(e) => e.getNestedType
            case x:ClassType => x
        }
    }
}
