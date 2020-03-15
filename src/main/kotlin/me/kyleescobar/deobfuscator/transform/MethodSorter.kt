package me.kyleescobar.deobfuscator.transform

import me.kyleescobar.deobfuscator.asm.ClassGroup
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode

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

/**
 * Sorts methods in each class by line number.
 */
class MethodSorter : Transformer {

    override fun transform(group: ClassGroup) {
        group.forEach { c ->
            val lineNumbers = c.methods.associate { it to (it.firstLineNumber() ?: Integer.MAX_VALUE) }
            c.methods = c.methods.sortedBy { lineNumbers.getValue(it) }
        }
    }

    private fun MethodNode.firstLineNumber(): Int? {
        for(insn in instructions) {
            if(insn is LineNumberNode) {
                return insn.line
            }
        }
        return null
    }
}