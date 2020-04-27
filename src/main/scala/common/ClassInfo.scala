package common

import astnodes.TypedVar
import astnodes.types.{ClassType, FuncType, NoneType, PyType, ValueType}

import scala.collection.mutable.ListBuffer

/**
 * class information of python class
 *
 * @param className String, class name
 * @param baseClass ClassInfo, class reference of base class, if None, then Object, root of types
 * @param funcTable List of function information
 */
case class ClassInfo(className: String, baseClass: Option[ClassInfo], varTable: List[TypedVar], funcTable: List[FuncType]) {

    var constructor:FuncType = FuncType("__init__", Nil, NoneType())

    def getVarMember(name: String): Option[TypedVar] = {
        varTable.find(_.identifier.name == name) match {
            case Some(v) => Some(v)
            case None => baseClass.flatMap(_.getVarMember(name))
        }
    }

    def getFuncMember(name: String): Option[FuncType] = {
        funcTable.find(_.name == name) match {
            case Some(f) => Some(f)
            case None => baseClass.flatMap(_.getFuncMember(name))
        }
    }

    def defaultConstructor(): FuncType = {
        FuncType("__init__", Nil, NoneType())
    }

    override def toString: String = {
        s"Class<$className>"
    }
}

object ClassInfo {

    def isNativeType(className: String): Boolean = {
        className == "object" || className == "int" || className == "float" || className == "type" ||
        className == "list" || className == "str" || className == "tuple" || className == "None" || className == "bool"
    }

    // type
    val klassType: ClassInfo = ClassInfo("type", None, Nil, List(
        FuncType("__eq__", List[ValueType](ClassType.metaType), ClassType.boolType),
    ))

    // object
    val klassObject: ClassInfo = ClassInfo("object", None, Nil, List(
        FuncType("__bool__", Nil, ClassType.boolType),
        FuncType("__eq__", List[ValueType](ClassType.objectType), ClassType.boolType),
        FuncType("__is__", List[ValueType](ClassType.objectType), ClassType.boolType),
    ))

    // bool
    val klassBool: ClassInfo = ClassInfo("bool", Some(klassObject), Nil, List(
        FuncType("__bool__", Nil, ClassType.boolType),
    ))

    // float
    val klassFloat: ClassInfo = ClassInfo("float", Some(klassObject), Nil, List(
        FuncType("__bool__", Nil, ClassType.boolType),
        FuncType("__eq__", List(ClassType.objectType), ClassType.boolType),
        FuncType("__pos__", Nil, ClassType.floatType),
        FuncType("__ng__", Nil, ClassType.floatType),
        FuncType("__add__", List(ClassType.floatType), ClassType.floatType),
        FuncType("__sub__", List(ClassType.floatType), ClassType.floatType),
        FuncType("__mul__", List(ClassType.floatType), ClassType.floatType),
        FuncType("__ge__", List(ClassType.floatType), ClassType.boolType), // >=
        FuncType("__le__", List(ClassType.floatType), ClassType.boolType), // <=
        FuncType("__gt__", List(ClassType.floatType), ClassType.boolType), // >
        FuncType("__lt__", List(ClassType.floatType), ClassType.boolType), // <
    ))

    // int
    val klassInt: ClassInfo = ClassInfo("int", Some(klassObject), Nil, List(
        FuncType("__bool__", Nil, ClassType.boolType),
        FuncType("__eq__", List(ClassType.objectType), ClassType.boolType),
        FuncType("__pos__", Nil, ClassType.intType),
        FuncType("__ng__", Nil, ClassType.intType),
        FuncType("__add__", List(ClassType.intType), ClassType.intType),
        FuncType("__sub__", List(ClassType.intType), ClassType.intType),
        FuncType("__mul__", List(ClassType.intType), ClassType.intType),
        FuncType("__ge__", List(ClassType.intType), ClassType.boolType), // >=
        FuncType("__le__", List(ClassType.intType), ClassType.boolType), // <=
        FuncType("__gt__", List(ClassType.intType), ClassType.boolType), // >
        FuncType("__lt__", List(ClassType.intType), ClassType.boolType), // <
    ))

    // list
    val klassList: ClassInfo = ClassInfo("list", Some(klassObject), Nil, List(
        FuncType("__bool__", Nil, ClassType.boolType),
        FuncType("__len__", Nil, ClassType.intType),
        FuncType("__add__", List(ClassType.listType), ClassType.listType),
        FuncType("__mul__", List(ClassType.intType), ClassType.listType),
        FuncType("__in__", List(ClassType.objectType), ClassType.boolType),
    ))

    // tuple
    val klassTuple: ClassInfo = ClassInfo("tuple", Some(klassObject), Nil, List(
        FuncType("__bool__", Nil, ClassType.boolType),
        FuncType("__len__", Nil, ClassType.intType),
    ))

    // str
    val klassStr: ClassInfo = ClassInfo("str", Some(klassObject), Nil, List(
        FuncType("__bool__", Nil, ClassType.boolType),
        FuncType("__len__", Nil, ClassType.intType),
        FuncType("__add__", List(ClassType.strType), ClassType.strType),
    ))

    // none
    val klassNone: ClassInfo = ClassInfo("None", None, Nil, List(
        FuncType("__bool__", Nil, ClassType.boolType),
        FuncType("__is__", List[ValueType](ClassType.objectType), ClassType.boolType),
        FuncType("__eq__", List[ValueType](ClassType.objectType), ClassType.boolType),
    ))
}
