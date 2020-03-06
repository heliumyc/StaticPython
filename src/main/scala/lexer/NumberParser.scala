package lexer

import scala.collection.mutable.ListBuffer

class NumberParser {
    case class Transition(f:Char=>Boolean, toState: State)
    type ReturnType = (String, Int)

    case class State(id:Int) {
        val transitionList: ListBuffer[Transition] = new ListBuffer[Transition]()
        def ->: (next: Char): Transition = {
            Transition(_==next, this)
        }

        def ->: (nextF: Char=>Boolean): Transition = {
            Transition(nextF(_), this)
        }

        def := (transition: Transition): Unit = {
            transition +=: transitionList
        }
    }

    @inline def isNonZeroDigit(c: Char): Boolean = c >= '1' && c <= '9'
    @inline def isDigit(c: Char):Boolean = c >= '0' && c <= '9'
    @inline def isExp(c: Char):Boolean = c == 'e' || c == 'E'
    @inline def isOp(c: Char):Boolean = c == '+' || c == '-'

    // definition
    val numberLexeme: StringBuilder = new StringBuilder()
    val states: IndexedSeq[State] = (0 to 9).map(State) // number of states in automaton
    val initialState: State = states(0)
    val acceptStateActionMap: Map[State, () => ReturnType] = Map[State, ()=> ReturnType](states(1) -> integerAction, states(2) -> integerAction,
        states(5) -> floatPointAction, states(6) -> floatPointAction, states(9) -> floatPointAction)
    val errorState: State = State(-1)

    // rules
    states(0) := '0' ->: states(1)
    states(0) := isNonZeroDigit _ ->: states(2)
    states(0) := '.' ->: states(3)

    states(1) := '0' ->: states(1)
    states(1) := isNonZeroDigit _ ->: states(4)
    states(1) := '.' ->: states(5)
    states(1) := isExp _ ->: states(7)

    states(2) := isDigit _ ->: states(2)
    states(2) := '.' ->: states(5)
    states(2) := isExp _ ->: states(7)

    states(3) := isDigit _ ->: states(6)

    states(4) := isDigit _ ->: states(4)
    states(4) := '.' ->: states(5)
    states(4) := isExp _ ->: states(7)

    states(5) := isDigit _ ->: states(5)
    states(5) := isExp _ ->: states(7)

    states(6) := isDigit _ ->: states(6)
    states(6) := isExp _ ->: states(7)

    states(7) := isOp _ ->: states(8)
    states(7) := isDigit _ ->: states(9)

    states(8) := isDigit _ ->: states(9)

    states(9) := isDigit _ ->: states(9)

    // actions
    def integerAction(): ReturnType = new ReturnType(numberLexeme.toString(), 1)

    def floatPointAction(): ReturnType = new ReturnType(numberLexeme.toString(), 2)

    def errorAction(): ReturnType = new ReturnType("", -1)

    // main loop
    def parse(input: String): ReturnType = {
        def stop(state: State) = {
            if (acceptStateActionMap.contains(state)) {
                acceptStateActionMap(state)()
            } else {
                errorAction()
            }
        }
        numberLexeme.clear()
        var curState = states(0)
        for (c <- input) {
            curState.transitionList.find(_.f(c)) match {
                case Some(Transition(_, toState)) =>
                    numberLexeme += c
                    curState = toState
                case None =>
                    return stop(curState)
            }
        }
        stop(curState)
    }
}
