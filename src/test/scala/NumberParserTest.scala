import junit.framework.TestCase
import lexer.NumberParser
import org.junit.Assert._

class NumberParserTest extends TestCase {

    var parser:NumberParser = _
    override def setUp() {
        parser = new NumberParser
    }

    def testGoodNumbers(): Unit = {
        assertEquals(("2147483647", 1), parser.parse("2147483647"))
        assertEquals(("79228162514264337593543950336", 1), parser.parse("79228162514264337593543950336"))
        assertEquals(("000", 1), parser.parse("000"))
        assertEquals(("3.14", 2), parser.parse("3.14 "))
        assertEquals(("10.", 2), parser.parse("10. "))
        assertEquals(("0.", 2), parser.parse("0. "))
        assertEquals((".0", 2), parser.parse(".0 "))
        assertEquals((".001", 2), parser.parse(".001"))
        assertEquals(("1e100", 2), parser.parse("1e100"))
        assertEquals(("3.14e-10", 2), parser.parse("3.14e-10"))
        assertEquals((".14e-10", 2), parser.parse(".14e-10"))
        assertEquals(("00.00e+10", 2), parser.parse("00.00e+10"))
        assertEquals(("0e0", 2), parser.parse("0e0"))
    }

    def testBadNumbers(): Unit = {
        assertEquals(("", -1), parser.parse("1e"))
        assertEquals(("", -1), parser.parse(".   "))
        assertEquals(("", -1), parser.parse("002"))
        assertEquals(("", -1), parser.parse("10.e"))
        assertEquals(("", -1), parser.parse("234734e-"))
        assertEquals(("", -1), parser.parse("1ee"))
        assertEquals(("", -1), parser.parse("1e++"))
    }

    def testMiscellaneous(): Unit = {
        assertEquals(("1e2", 2), parser.parse("1e2+"))
        assertEquals(("234321", 1), parser.parse("234321+1.0"))
        assertEquals((".0", 2), parser.parse(".0++"))
        assertEquals(("10.2e+0", 2), parser.parse("10.2e+0-2"))
    }

}
