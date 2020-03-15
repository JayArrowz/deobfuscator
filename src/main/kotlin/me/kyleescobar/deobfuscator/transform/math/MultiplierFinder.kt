package me.kyleescobar.deobfuscator.transform.math

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import me.kyleescobar.deobfuscator.asm.ClassGroup
import me.kyleescobar.deobfuscator.asm.util.invert
import me.kyleescobar.deobfuscator.asm.util.isInvertible
import me.kyleescobar.deobfuscator.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.Type.INT_TYPE
import org.objectweb.asm.Type.LONG_TYPE
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.*

/**
 * Copyright (c) 2020 Kyle Escobar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

class MultiplierFinder(private val group: ClassGroup) {

    val decoders = MultimapBuilder.hashKeys().arrayListValues().build<String, Number>()
    val dependentDecoders = MultimapBuilder.hashKeys().arrayListValues().build<String, Pair<String, Number>>()
    val dependentEncoders = MultimapBuilder.hashKeys().arrayListValues().build<String, Pair<String, Number>>()

    private val analyzer = Analyzer(Inter(decoders, dependentDecoders, dependentEncoders))

    fun findMultipliers() {
        group.forEach { c ->
            c.methods.forEach { m ->
                findDupPutDecoders(m, decoders)
                analyzer.analyze(c.name, m)
            }
        }
    }

    private fun isMultiplier(n: Number): Boolean {
        return isInvertible(n) && invert(n) != n
    }

    private fun findDupPutDecoders(m: MethodNode, decoders: Multimap<String, Number>) {
        for(insn in m.instructions) {
            val ldc = insn as? LdcInsnNode ?: continue
            val cst = ldc.cst
            if(cst !is Int && cst !is Long) continue
            val put = ldc.previous as? FieldInsnNode ?: continue
            val dup = put.previous ?: continue
            val mul = ldc.next ?: continue
            if(mul.opcode != IMUL && mul.opcode != LMUL) continue
        }
    }

    private class Inter(
        val decoders: Multimap<String, Number>,
        val dependentDecoders: Multimap<String, Pair<String, Number>>,
        val dependentEncoders: Multimap<String, Pair<String, Number>>
    ) : Interpreter<Expr>(Opcodes.ASM7) {

        private val basicInterpreter = BasicInterpreter()

        override fun binaryOperation(insn: AbstractInsnNode, value1: Expr, value2: Expr): Expr? {
            val bv = basicInterpreter.binaryOperation(insn, value1.basicValue, value2.basicValue)
            return when(insn.opcode) {
                IMUL, LMUL -> {
                    val ldc = value1 as? Expr.IntegerLdc ?: value2 as? Expr.IntegerLdc ?: return Expr.Var(bv)
                    val gf = value1 as? Expr.GetField ?: value2 as? Expr.GetField ?: return Expr.LdcMult(bv, ldc.n)
                    if(isInvertible(ldc.n) && invert(ldc.n) != ldc.n) {
                        decoders.put("${gf.insn.owner}.${gf.insn.name}", ldc.n)
                    }
                    Expr.GetFieldMult(bv, gf.insn, ldc.n)
                }
                PUTFIELD -> {
                    putField(insn as FieldInsnNode, value2)
                    null
                }
                else -> bv?.let { Expr.Var(it) }
            }
        }

        override fun copyOperation(insn: AbstractInsnNode, value: Expr): Expr = value

        override fun merge(value1: Expr, value2: Expr): Expr {
            return if(value1.basicValue == value2.basicValue) {
                value1
            } else {
                Expr.Var(basicInterpreter.merge(value1.basicValue, value2.basicValue))
            }
        }

        override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out Expr>?): Expr? {
            val bv = basicInterpreter.naryOperation(insn, emptyList()) ?: return null
            return Expr.Var(bv)
        }

        override fun newOperation(insn: AbstractInsnNode): Expr {
            val bv = basicInterpreter.newOperation(insn)
            return when(insn.opcode) {
                LDC -> {
                    insn as LdcInsnNode
                    when(insn.cst) {
                        is Int, is Long -> Expr.IntegerLdc(bv, insn.cst as Number)
                        else -> Expr.Var(bv)
                    }
                }
                GETSTATIC -> getField(bv, insn as FieldInsnNode)
                else -> Expr.Var(bv)
            }
        }

        override fun newValue(type: Type?): Expr? {
            val sv = basicInterpreter.newValue(type) ?: return null
            return Expr.Var(sv)
        }

        override fun returnOperation(insn: AbstractInsnNode, value: Expr, expected: Expr) {}
        override fun ternaryOperation(insn: AbstractInsnNode, value1: Expr, value2: Expr, value3: Expr): Expr? = null

        override fun unaryOperation(insn: AbstractInsnNode, value: Expr): Expr? {
            val bv = basicInterpreter.unaryOperation(insn, value.basicValue)
            return when(insn.opcode) {
                GETFIELD -> getField(bv, insn as FieldInsnNode)
                PUTSTATIC -> {
                    putField(insn as FieldInsnNode, value)
                    null
                }
                else -> bv?.let { Expr.Var(it) }
            }
        }

        private fun getField(bv: BasicValue, insn: FieldInsnNode): Expr {
            return when(insn.desc) {
                INT_TYPE.descriptor, LONG_TYPE.descriptor -> Expr.GetField(bv, insn)
                else -> Expr.Var(bv)
            }
        }

        private fun putField(insn: FieldInsnNode, value: Expr) {
            val destName = "${insn.owner}.${insn.name}"
            if(value is Expr.IntegerLdc) {
                // TODO Unfold multiplier values.
            } else if(value is Expr.LdcMult) {
                if(isInvertible(value.n) && invert(value.n) != value.n) {
                    decoders.put(destName, invert(value.n))
                }
            } else if(value is Expr.GetFieldMult) {
                val srcName = "${value.insn.owner}.${value.insn.name}"
                decoders.remove(srcName, value.n)
                dependentDecoders.put(srcName, destName to value.n)
                dependentEncoders.put(srcName, srcName to value.n)
            }
        }
    }

    private sealed class Expr : Value {
        override fun getSize(): Int = basicValue.size

        abstract val basicValue: BasicValue

        data class Var(override val basicValue: BasicValue): Expr()

        data class IntegerLdc(override val basicValue: BasicValue, val n: Number): Expr()

        data class GetField(override val basicValue: BasicValue, val insn: FieldInsnNode): Expr()

        data class GetFieldMult(override val basicValue: BasicValue, val insn: FieldInsnNode, val n: Number): Expr()

        data class LdcMult(override val basicValue: BasicValue, val n: Number) : Expr()

    }
}