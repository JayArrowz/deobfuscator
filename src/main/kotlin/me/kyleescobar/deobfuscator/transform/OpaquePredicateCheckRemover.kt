package me.kyleescobar.deobfuscator.transform

import me.kyleescobar.deobfuscator.asm.ClassGroup
import me.kyleescobar.deobfuscator.asm.constantIntProduced
import me.kyleescobar.deobfuscator.asm.isConstantIntProducer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.tinylog.kotlin.Logger
import java.lang.IllegalStateException
import java.lang.reflect.Modifier
import java.util.*

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

class OpaquePredicateCheckRemover : Transformer {

    private val args = TreeMap<String, Int>()
    private var returns = 0
    private var exceptions = 0

    override fun transform(group: ClassGroup) {
        group.forEach { c ->
            c.methods.forEach { m ->
                val instructions = m.instructions.iterator()
                val lastIndex = m.lastParamIndex
                while(instructions.hasNext()) {
                    val insn = instructions.next()
                    val toDelete = if (insn.matchesReturn(lastIndex)) {
                        returns++
                        4
                    } else if (insn.matchesException(lastIndex)) {
                        exceptions++
                        7
                    } else {
                        continue
                    }

                    val pushed = insn.next.constantIntProduced
                    val ifOpcode = insn.next.next.opcode
                    val label = (insn.next.next as JumpInsnNode).label.label
                    instructions.remove()
                    repeat(toDelete - 1) {
                        instructions.next()
                        instructions.remove()
                    }

                    instructions.add(JumpInsnNode(GOTO, LabelNode(label)))
                    args[c.name + "." + m.name + m.desc] = passingVal(pushed, ifOpcode)
                }
            }
        }

        Logger.info("Opaque predicate checks removed: [returns=$returns, exceptions=$exceptions].")
    }

    private val MethodNode.lastParamIndex: Int get() {
        val offset = if(Modifier.isStatic(access)) 1 else 0
        return (Type.getArgumentsAndReturnSizes(desc) shr 2) - offset - 1
    }

    private fun AbstractInsnNode.matchesReturn(lastIndex: Int): Boolean {
        val i0 = this
        if(i0.opcode != ILOAD) return false
        i0 as VarInsnNode
        if(i0.`var` != lastIndex) return false
        val i1 = i0.next
        if(!i1.isConstantIntProducer) return false
        val i2 = i1.next
        if(!i2.isIf) return false
        val i3 = i2.next
        if(!i3.isReturn) return false
        return true
    }

    private fun AbstractInsnNode.matchesException(lastIndex: Int): Boolean {
        val i0 = this
        if(i0.opcode != ILOAD) return false
        i0 as VarInsnNode
        if(i0.`var` != lastIndex) return false
        val i1 = i0.next
        if(!i1.isConstantIntProducer) return false
        val i2 = i1.next
        if(!i2.isIf) return false
        val i3 = i2.next
        if(i3.opcode != NEW) return false
        val i4 = i3.next
        if(i4.opcode != DUP) return false
        val i5 = i4.next
        i5 as MethodInsnNode
        if(i5.owner != Type.getInternalName(IllegalStateException::class.java)) return false
        val i6 = i5.next
        if(i6.opcode != ATHROW) return false
        return true
    }

    private val AbstractInsnNode.isIf: Boolean get() {
        return this is JumpInsnNode && opcode != Opcodes.GOTO
    }

    private val AbstractInsnNode.isReturn: Boolean get() {
        return when(opcode) {
            RETURN, ARETURN, DRETURN, FRETURN, IRETURN, LRETURN -> true
            else -> false
        }
    }

    private fun passingVal(pushed: Int, ifOpcode: Int): Int {
        return when(ifOpcode) {
            IF_ICMPEQ -> pushed
            IF_ICMPGE, IF_ICMPGT -> pushed + 1
            IF_ICMPLE, IF_ICMPLT, IF_ICMPNE -> pushed - 1
            else -> error(ifOpcode)
        }
    }
}