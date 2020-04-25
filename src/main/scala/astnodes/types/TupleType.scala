package astnodes.types

case class TupleType(productType: List[ValueType]) extends ValueType {

    override def toString: String = s"${productType.mkString(",")}"
}
