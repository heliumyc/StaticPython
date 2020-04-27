package common

import astnodes.types.{ClassType, FuncType, ListType, NoneType, PyType, TupleType, ValueType}

import scala.collection.mutable

class ClassUtils(classDecls: SymbolTable[ClassInfo]) {

    /**
     * whether sourceType is compatible to targetType
     *
     * @param targetType compatible type
     * @param sourceType subtype to be test
     * @return
     */
    def isSubtype(targetType: PyType, sourceType: PyType): Boolean = {
        targetType match {
            case ListType(t1) =>
                sourceType match {
                    case ListType(t2) =>
                        if (t2 == NoneType()) true else isSubtype(t1, t2)
                    case NoneType() => true
                    case _ => false
                }
            case TupleType(productType_1) =>
                sourceType match {
                    case TupleType(productType_2) => productType_1 == productType_2
                    case NoneType() => true
                    case _ => false
                }
            case _: FuncType => false
            case NoneType() => sourceType == NoneType()
            case c1: ClassType =>
                sourceType match {
                    case c2: ClassType => isClassSubType(c1, c2)
                    case NoneType() => c1.name != "int" && c1.name != "float"
                    case _ => false
                }
        }
    }

    /**
     * only check if class subtyping
     *
     * @param parentType compatible parent type
     * @param subType    subtype to check
     * @return
     */
    def isClassSubType(parentType: ClassType, subType: ClassType): Boolean = {
        @scala.annotation.tailrec
        def _isClassSubType(parentClass: ClassInfo, subClass: ClassInfo): Boolean = {
            if (parentClass.className == subClass.className) true
            else {
                subClass.baseClass match {
                    case Some(c) => _isClassSubType(parentClass, c)
                    case None => false
                }
            }
        }

        classDecls.isDeclared(parentType.name) &&
            classDecls.isDeclared(subType.name) &&
            _isClassSubType(classDecls.get(parentType.name).get, classDecls.get(subType.name).get)
    }

    /**
     * whether all args fit in params
     *
     * @param params , defined types
     * @param args   , passed types
     * @return
     */
    def isArgsSubtype(params: List[PyType], args: List[PyType]): Boolean = {
        if (params.length == args.length)
            (params, args).zipped.forall((t1, t2) => isSubtype(t1, t2))
        else false
    }

    /**
     * basically LCA, BUT, i would use naive algo because node in this can only access parent
     * and it is slightly hard to come up with a Tarjan for this kind of tree
     * again premature optimization is the root of evil
     *
     * @param left  type 1
     * @param right type 2
     * @return
     */
    def findLeastCommonBaseClass(left: ValueType, right: ValueType): ValueType = {
        @scala.annotation.tailrec
        def _findDepth(c: ClassInfo, acc: Int): Int = {
            if (c.baseClass.isEmpty) acc
            else _findDepth(c.baseClass.get, acc + 1)
        }

        def _findClassLCA(left: ClassInfo, right: ClassInfo): ClassInfo = {
            var leftDepth = _findDepth(left, 0)
            var rightDepth = _findDepth(right, 0)

            var _left = left
            var _right = right
            if (leftDepth > rightDepth) {
                while (leftDepth != rightDepth) {
                    _left = _left.baseClass.get
                    leftDepth -= 1
                }
            } else if (leftDepth < rightDepth) {
                while (leftDepth != rightDepth) {
                    _right = _right.baseClass.get
                    rightDepth -= 1
                }
            }

            while (_left != _right) {
                _left = _left.baseClass.get
                _right = _right.baseClass.get
            }

            _left
        }

        left match {
            case ClassType(c1) =>
                right match {
                    case ClassType(c2) => ClassType(_findClassLCA(classDecls.get(c1).get, classDecls.get(c2).get).className)
                    case _ => ClassType.objectType
                }
            case ListType(innerLeft) =>
                right match {
                    case ListType(innerRight) => if (innerLeft == innerRight) ListType(innerLeft) else ClassType.objectType
                    case _ => ClassType.objectType
                }
            case TupleType(leftProd) =>
                right match {
                    case TupleType(rightProd) => if (leftProd == rightProd) TupleType(leftProd) else ClassType.objectType
                    case _ => ClassType.objectType
                }
            case NoneType() => right
        }
    }

    def findMethod(targetClass: ClassInfo, methodName: String, args: List[PyType]): Option[FuncType] = {
        targetClass.funcTable.find(_.name == methodName) match {
            case Some(x) if isArgsSubtype(x.params, args) => Some(x)
            case _ =>
                if (targetClass != ClassInfo.klassNone)
                    targetClass.baseClass.flatMap(findMethod(_, methodName, args))
                else None
        }
    }
}

object ClassUtils {
    def apply(classDecls: SymbolTable[ClassInfo]) = new ClassUtils(classDecls)
}
