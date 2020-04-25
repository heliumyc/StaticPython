package common

import astnodes.types.{ClassType, FuncType, ListType, PyType, ValueType}
import scala.collection.mutable

/**
 * symbol table which stores function and variable symbols
 *
 * @param parent possible parent table, None means no parent that is the top level
 */
class SymbolTable[T](val parent: Option[SymbolTable[T]]) {
    val table: mutable.Map[String, T] = mutable.HashMap[String, T]()

    def get(name: String): Option[T] = {
        table.get(name) match {
            case x@Some(_) => x
            case None => parent.flatMap(_.get(name))
        }
    }

    def put(name: String, value: T): Unit = {
        table.put(name, value)
    }

    def isDeclared(name: String): Boolean = table.contains(name)

    /*
        find name along all the available scope
     */
    @scala.annotation.tailrec
    final def findName(name: String): Boolean = {
        if (!this.isDeclared(name)) {
            this.parent match {
                case Some(s) => s.findName(name)
                case None => false
            }
        } else true
    }
}

object SymbolTable {

    def getDefaultClassTable: SymbolTable[ClassInfo] = {
        val classTable = new SymbolTable[ClassInfo](None)
        // list type is not accessible to user, type annotation must be [T], type constructor is a must
        List(ClassInfo.klassObject, ClassInfo.klassInt, ClassInfo.klassFloat,
            ClassInfo.klassStr, ClassInfo.klassBool).foreach(x => classTable.put(x.className, x))
        classTable
    }

    def getDefaultGlobalSyms: SymbolTable[PyType] = {
        // add some native symbols and functions
        val defaultGlobalSyms = new SymbolTable[PyType](None)
        defaultGlobalSyms.put("range", FuncType("range", List(ClassType("int"), ClassType("int")), ListType(ClassType("int"))))
        defaultGlobalSyms.put("type", FuncType("type", List(ClassType("object")), ClassType("type")))
        defaultGlobalSyms.put("id", FuncType("id", List(ClassType("int"), ClassType("int")), ClassType("int")))
        defaultGlobalSyms.put("len", FuncType("len", List(ListType(ClassType("object"))), ClassType("int")))
        defaultGlobalSyms.put("print", FuncType("print", List(ClassType("object")), ClassType("None")))
        defaultGlobalSyms
    }

    def apply[T](parent: SymbolTable[T]): SymbolTable[T] = {
        new SymbolTable[T](Some(parent))
    }

}
