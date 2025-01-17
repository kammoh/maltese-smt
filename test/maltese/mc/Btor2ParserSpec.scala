// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package maltese.mc

import org.scalatest.flatspec.AnyFlatSpec

class Btor2ParserSpec extends AnyFlatSpec {
  // this example if from the official btor2tools repository
  private val count2 =
    """1 sort bitvec 3
      |2 zero 1
      |3 state 1
      |4 init 1 3 2
      |5 one 1
      |6 add 1 3 5
      |7 next 1 3 6
      |8 ones 1
      |9 sort bitvec 1
      |10 eq 9 3 8
      |11 bad 10
      |""".stripMargin

  it should "parse count2 w/o inlining" in {
    val expected =
      """counter2
        |node s2 : bv<3> = 3'b0
        |init _state_0.init : bv<3> = s2
        |node s5 : bv<3> = 3'b1
        |node s6 : bv<3> = add(_state_0, s5)
        |next _state_0.next : bv<3> = s6
        |node s8 : bv<3> = 3'b111
        |node s10 : bv<1> = eq(_state_0, s8)
        |bad _bad_0 : bv<1> = s10
        |state _state_0 : bv<3>
        |  [init] _state_0.init
        |  [next] _state_0.next
        |""".stripMargin
    val sys = Btor2.read(count2, inlineSignals = false, defaultName = "counter2").serialize
    assert(sys.trim == expected.trim)
  }

  it should "parse count2 with inlining" in {
    val expected =
      """counter2
        |bad _bad_0 : bv<1> = eq(_state_0, 3'b111)
        |state _state_0 : bv<3>
        |  [init] 3'b0
        |  [next] add(_state_0, 3'b1)
        |""".stripMargin
    val sys = Btor2.read(count2, inlineSignals = true, defaultName = "counter2").serialize
    assert(sys.trim == expected.trim)
  }
}
