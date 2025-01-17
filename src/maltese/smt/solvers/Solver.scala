// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package maltese.smt.solvers

import maltese.smt._

object Solver {
  type Logic = SMTFeature.ValueSet
}

trait Solver {
  // basic properties
  def name:                String
  def supportsQuantifiers: Boolean

  /** Constant Arrays are not required by SMTLib: https://rise4fun.com/z3/tutorialcontent/guide */
  def supportsConstArrays:            Boolean
  def supportsUninterpretedFunctions: Boolean

  // basic API
  import Solver.Logic
  def setLogic(logic: Logic): Unit = {
    require(supportsQuantifiers || logic.contains(SMTFeature.QuantifierFree), s"$name does not support quantifiers!")
    require(
      supportsUninterpretedFunctions || !logic.contains(SMTFeature.UninterpretedFunctions),
      s"$name does not support uninterpreted functions!"
    )
    doSetLogic(logic)
    pLogic = Some(logic)
  }
  def getLogic: Option[Logic] = pLogic
  def stackDepth: Int // returns the size of the push/pop stack
  def push():     Unit
  def pop():      Unit
  def assert(expr: BVExpr): Unit
  final def check(produceModel: Boolean): SolverResult = {
    require(pLogic.isDefined, "Use `setLogic` to select the logic.")
    pCheckCount += 1
    doCheck(produceModel)
  }
  def runCommand(cmd: SMTCommand): Unit
  def queryModel(e:   BVSymbol):   Option[BigInt]
  def getValue(e:     BVExpr):     Option[BigInt]
  def getValue(e:     ArrayExpr):  Seq[(Option[BigInt], BigInt)]

  /** releases all native resources */
  def close(): Unit

  // convenience API
  def check(): SolverResult = check(true)
  def check(expr: BVExpr): SolverResult = check(expr, true)
  def check(expr: BVExpr, produceModel: Boolean): SolverResult = {
    push()
    assert(expr)
    val res = check(produceModel)
    pop()
    res
  }

  // statistics
  def checkCount: Int = pCheckCount
  private var pCheckCount = 0

  // internal functions that need to be implemented by the solver
  private var pLogic: Option[Logic] = None
  protected def doSetLogic(logic:     Logic):   Unit
  protected def doCheck(produceModel: Boolean): SolverResult
}

/** SMTLib theories + QF */
object SMTFeature extends Enumeration {
  val BitVector, Array, UninterpretedFunctions, QuantifierFree = Value
  def toName(logic: ValueSet): String = {
    val a = if (logic.contains(Array)) "A" else ""
    val uf = if (logic.contains(UninterpretedFunctions)) "UF" else ""
    val bv = if (logic.contains(BitVector)) "BV" else ""
    val theories = a + uf + bv
    require(AllowedTheories.contains(theories), s"Unsupported theory combination: $theories")
    val prefix = if (logic.contains(QuantifierFree)) "QF_" else ""
    prefix + theories
  }
  private val AllowedTheories = Set("ABV", "AUFBV", "BV", "UF", "UFBV")
}

sealed trait SolverResult {
  def isSat:   Boolean = false
  def isUnSat: Boolean = false
}
case object IsSat extends SolverResult { override def isSat = true }
case object IsUnSat extends SolverResult { override def isUnSat = true }
case object IsUnknown extends SolverResult
