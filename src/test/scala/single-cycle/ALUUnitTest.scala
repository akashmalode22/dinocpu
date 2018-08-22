// Unit tests for the ALU

package edu.darchr.codcpu

import chisel3._

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


class ALUUnitTester(c: ALU) extends PeekPokeTester(c) {
  private val alu = c

  val maxInt = BigInt("FFFFFFFF", 16)

  def test(op: UInt, f: (BigInt, BigInt) => BigInt) {
    for (i <- 0 until 10) {
      val x = rnd.nextInt(100)
      val y = rnd.nextInt(500)
      poke(alu.io.operation, op)
      poke(alu.io.inputx, x)
      poke(alu.io.inputy, y)
      step(1)
      val expectOut = f(x, y)
      val expectZero = (expectOut == 0)
      expect(alu.io.result, expectOut)
      expect(alu.io.zero, expectZero)
    }
  }

  def twoscomp(v: BigInt) : BigInt = {
    if (v < 0) {
      return maxInt + v + 1
    } else {
      return v
    }
  }

  test(alu.ADD_OP, (x: BigInt, y: BigInt) => (x + y))
  test(alu.AND_OP, (x: BigInt, y: BigInt) => (x & y))
  test(alu.OR_OP, (x: BigInt, y: BigInt) => (x | y))
  test(alu.SUB_OP, (x: BigInt, y: BigInt) => twoscomp(x - y))
  test(alu.SLT_OP, (x: BigInt, y: BigInt) => (if (x < y) 1 else 0))
  test(alu.NOR_OP, (x: BigInt, y: BigInt) => twoscomp(~(x | y)))

  // Explicitly test 0
  val x = 0
  val y = 0
  poke(alu.io.operation, alu.ADD_OP)
  poke(alu.io.inputx, x)
  poke(alu.io.inputy, y)
  step(1)
  expect(alu.io.result, x+y)
  expect(alu.io.zero, (x+y == 0))
  poke(alu.io.operation, alu.AND_OP)
  poke(alu.io.inputx, x)
  poke(alu.io.inputy, y)
  step(1)
  expect(alu.io.result, 0)
  expect(alu.io.zero, 1)
  poke(alu.io.operation, alu.OR_OP)
  poke(alu.io.inputx, x)
  poke(alu.io.inputy, y)
  step(1)
  expect(alu.io.result, 0)
  expect(alu.io.zero, 1)
  poke(alu.io.operation, alu.SUB_OP)
  poke(alu.io.inputx, x)
  poke(alu.io.inputy, y)
  step(1)
  expect(alu.io.result, x-y)
  expect(alu.io.zero, (x-y == 0))
  poke(alu.io.operation, alu.SLT_OP)
  poke(alu.io.inputx, x)
  poke(alu.io.inputy, y)
  step(1)
  expect(alu.io.result, 0)
  expect(alu.io.zero, (1))
  poke(alu.io.operation, alu.NOR_OP)
  poke(alu.io.inputx, x)
  poke(alu.io.inputy, y)
  step(1)
  expect(alu.io.result, maxInt)
  expect(alu.io.zero, (0))
}

/**
  * This is a trivial example of how to run this Specification
  * From within sbt use:
  * {{{
  * testOnly edu.darchr.codcpu.ALUTester
  * }}}
  * From a terminal shell use:
  * {{{
  * sbt 'testOnly edu.darchr.codcpu.ALUTester'
  * }}}
  */
class ALUTester extends ChiselFlatSpec {
  private val backendNames = if(firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "ALU" should s"save written values (with $backendName)" in {
      Driver(() => new ALU, backendName) {
        c => new ALUUnitTester(c)
      } should be (true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array(), () => new ALU) {
      c => new ALUUnitTester(c)
    } should be (true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new ALU) {
        c => new ALUUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new ALU) {
      c => new ALUUnitTester(c)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from your test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new ALU) {
      c => new ALUUnitTester(c)
    } should be(true)
  }
}