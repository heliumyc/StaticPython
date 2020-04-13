package common

import astnodes.types.FuncType

/**
 * class information of python clas
 *
 * @param className String, class name
 * @param baseClass ClassInfo, class reference of base class, if None, then Object, root of types
 * @param funcTable List of function information
 */
case class ClassInfo(className: String, baseClass: Option[ClassInfo], funcTable: List[FuncType])
