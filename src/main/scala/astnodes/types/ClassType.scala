package astnodes.types

case class ClassType(name: String) extends ValueType {

    override def toString: String = s"<class '$name'>"

}

object ClassType {
    val metaType: ClassType = ClassType("type")
    val objectType: ClassType = ClassType("object")
    val boolType: ClassType = ClassType("bool")
    val intType: ClassType = ClassType("int")
    val floatType: ClassType = ClassType("float")
    val listType: ClassType = ClassType("list")
    val tupleType: ClassType = ClassType("tuple")
    val strType: ClassType = ClassType("str")
    val noneType: ClassType = ClassType("None")
}
