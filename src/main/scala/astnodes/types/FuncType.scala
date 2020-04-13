package astnodes.types

import scala.collection.mutable.ListBuffer

/**
 * function signature: name + params + return type
 *
 * @param name String, name of the function
 * @param params List of ValueType, note that the order of types matters
 * @param returnType ValueType, return type of this function
 */
case class FuncType(name: String, params: List[ValueType], returnType: ValueType) extends PyType
