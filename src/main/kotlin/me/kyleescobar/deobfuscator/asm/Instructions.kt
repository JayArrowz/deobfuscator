package me.kyleescobar.deobfuscator.asm

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

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList

fun InsnList.insertSafe(previousInsn: AbstractInsnNode, vararg insns: AbstractInsnNode) {
    check(contains(previousInsn))
    insns.reversed().forEach { insert(previousInsn, it) }
}

fun InsnList.insertBeforeSafe(nextInsn: AbstractInsnNode, vararg insns: AbstractInsnNode) {
    check(contains(nextInsn))
    insns.forEach { insertBefore(nextInsn, it) }
}

fun InsnList.removeSafe(vararg insns: AbstractInsnNode) {
    insns.forEach {
        check(contains(it))
        remove(it)
    }
}

fun InsnList.setSafe(oldInsn: AbstractInsnNode, newInsn: AbstractInsnNode) {
    check(contains(oldInsn))
    set(oldInsn, newInsn)
}