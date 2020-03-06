package utils

import scala.collection.mutable

object Util {

    def removeFrontUntil[T](listBuffer: mutable.ListBuffer[T], f: T => Boolean): Int = {
        var count = 0
        while (listBuffer.nonEmpty && !f(listBuffer.head)) {
            listBuffer.remove(0)
            count += 1
        }
        count
    }
}
