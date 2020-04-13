package common

import astnodes.types.{ClassType, FuncType, ListType, PyType, ValueType}
import scala.collection.mutable

/**
 * symbol table which stores function and variable symbols
 * @param parent possible parent table, None means no parent that is the top level
 */
class SymbolTable[T](val parent: Option[SymbolTable[T]]) {
    private val table: mutable.Map[String, T] = mutable.HashMap[String, T]()

    def get(name: String): Option[T] = {
        table.get(name) match {
            case x@Some(_) => x
            case None => parent.flatMap(_.get(name))
        }
    }

    def put(name: String, value: T):Unit = {
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
    val defaultGlobalSyms = new SymbolTable[PyType](None)
    val classTable = new SymbolTable[ClassInfo](None)

    // add some native symbols and functions
    defaultGlobalSyms.put("range", FuncType("range", List(ClassType("int"), ClassType("int")), ListType(ClassType("int"))))
    defaultGlobalSyms.put("type", FuncType("type", List(ClassType("object")), ClassType("type")))
    defaultGlobalSyms.put("id", FuncType("id", List(ClassType("int"), ClassType("int")), ClassType("int")))
    defaultGlobalSyms.put("len", FuncType("len", List(ClassType("list")), ClassType("int")))
    defaultGlobalSyms.put("print", FuncType("print", List(ClassType("object")), ClassType("None")))

    // native types
    // type
    private val klassType = ClassInfo("type", None, List(
        FuncType("__eq__", List[ValueType](ClassType("type")), ClassType("bool")),
    ))

    // object
    private val klassObject = ClassInfo("object", None, List(
        FuncType("__eq__", List[ValueType](ClassType("object")), ClassType("bool")),
        FuncType("__bool__", Nil, ClassType("bool")),
    ))

    // bool
    private val klassBool = ClassInfo("bool", Some(klassObject), List(
        FuncType("__eq__", List[ValueType](ClassType("int")), ClassType("bool")),
        FuncType("__bool__", List[ValueType](ClassType("int")), ClassType("bool")),
    ))

    // int
    private val klassInt = ClassInfo("int", Some(klassObject), List(
        FuncType("__eq__", List[ValueType](ClassType("int")), ClassType("bool")),
        FuncType("__bool__", Nil, ClassType("bool")),
        FuncType("__lt__", List[ValueType](ClassType("int")), ClassType("bool")),
        FuncType("__gt__", List[ValueType](ClassType("int")), ClassType("bool")),
        FuncType("__le__", List[ValueType](ClassType("int")), ClassType("bool")),
        FuncType("__ge__", List[ValueType](ClassType("int")), ClassType("bool")),
    ))

    // float
    private val klassFloat = ClassInfo("float", Some(klassObject), List(
        FuncType("__eq__", List[ValueType](ClassType("float")), ClassType("bool")),
        FuncType("__bool__", Nil, ClassType("bool")),
        FuncType("__lt__", List[ValueType](ClassType("float")), ClassType("bool")),
        FuncType("__gt__", List[ValueType](ClassType("float")), ClassType("bool")),
        FuncType("__le__", List[ValueType](ClassType("float")), ClassType("bool")),
        FuncType("__ge__", List[ValueType](ClassType("float")), ClassType("bool")),
    ))

    // list
    private val klassList = ClassInfo("list", Some(klassObject), List(
        FuncType("__eq__", List[ValueType](ClassType("list")), ClassType("bool")),
        FuncType("__bool__", Nil, ClassType("bool")),
        FuncType("__len__", Nil, ClassType("int")),
    ))

    // str
    private val klassStr = ClassInfo("str", Some(klassObject), List(
        FuncType("__eq__", List[ValueType](ClassType("str")), ClassType("bool")),
        FuncType("__bool__", Nil, ClassType("bool")),
        FuncType("__len__", Nil, ClassType("int")),
    ))

    List(klassObject, klassInt, klassFloat, klassStr, klassList, klassBool, klassType).foreach(x=>classTable.put(x.className, x))

    def apply[T](parent:SymbolTable[T]):SymbolTable[T] = {
        new SymbolTable[T](Some(parent))
    }

}
