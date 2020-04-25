package astnodes

import astnodes.types.{PyType, ValueType}

trait Expression extends Statement {
    var inferredType: Option[PyType] = None
    var coercionType: Option[ValueType] = None

    def setInferredType(pyType: PyType): Option[PyType] = {
        inferredType = Some(pyType)
        inferredType
    }

    def setCoersionType(pyType: ValueType): Option[ValueType] = {
        coercionType = Some(pyType)
        coercionType
    }
}
