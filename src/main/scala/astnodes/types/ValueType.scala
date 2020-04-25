package astnodes.types

import common.ClassInfo

import scala.annotation.tailrec

trait ValueType extends PyType {

    @tailrec
    final def getNestedType: ValueType = {
        this match {
            case ListType(e) => e.getNestedType
            case x@_ => x
        }
    }
}
