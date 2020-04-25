package astnodes.types

case class ListType(elementType: ValueType) extends ValueType {

    override def toString: String = s"[$elementType]"
}
