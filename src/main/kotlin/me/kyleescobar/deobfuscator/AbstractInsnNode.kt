package me.kyleescobar.deobfuscator

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
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

val AbstractInsnNode.isConstantIntProducer: Boolean get() {
    return when(opcode) {
        Opcodes.LDC -> (this as LdcInsnNode).cst is Int
        Opcodes.SIPUSH, Opcodes.BIPUSH, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5, Opcodes.ICONST_M1 -> true
        else -> false
    }
}

val AbstractInsnNode.constantIntProduced: Int get() {
    return when(opcode) {
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