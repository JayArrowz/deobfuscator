package me.kyleescobar.deobfuscator.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode

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

fun pushConstIntInsn(n: Int): AbstractInsnNode {
    return when (n) {
        0 -> InsnNode(ICONST_0)
        1 -> InsnNode(ICONST_1)
        2 -> InsnNode(ICONST_2)
        3 -> InsnNode(ICONST_3)
        4 -> InsnNode(ICONST_4)
        5 -> InsnNode(ICONST_5)
        -1 -> InsnNode(ICONST_M1)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(BIPUSH, n)
        in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(SIPUSH, n)
        else -> LdcInsnNode(n)
    }
}

fun pushConstLongInsn(n: Long): AbstractInsnNode {
    return when (n) {
        0L -> InsnNode(LCONST_0)
        1L -> InsnNode(LCONST_1)
        else -> LdcInsnNode(n)
    }
}

val AbstractInsnNode.isConstantIntProducer: Boolean get() {
    return when (opcode) {
        LDC -> (this as LdcInsnNode).cst is Int
        SIPUSH, BIPUSH, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, ICONST_M1 -> true
        else -> false
    }
}

val AbstractInsnNode.constantIntProduced: Int get() {
    return when (opcode) {
        LDC -> (this as LdcInsnNode).cst as Int
        SIPUSH, BIPUSH -> (this as IntInsnNode).operand
        ICONST_0 -> 0
        ICONST_1 -> 1
        ICONST_2 -> 2
        ICONST_3 -> 3
        ICONST_4 -> 4
        ICONST_5 -> 5
        ICONST_M1 -> -1
        else -> error(this)
    }
}