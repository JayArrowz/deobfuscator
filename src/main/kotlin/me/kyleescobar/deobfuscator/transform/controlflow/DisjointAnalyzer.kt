package me.kyleescobar.deobfuscator.transform.controlflow

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue

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

class DisjointAnalyzer : Analyzer<BasicValue>(BasicInterpreter()) {

    val blocks = mutableListOf<Block>()

    override fun init(owner: String, method: MethodNode) {
        val insnList = method.instructions
        var currentBlock = Block()
        blocks.add(currentBlock)
        insnList.forEachIndexed { index, insn ->
            currentBlock.end++
            if(insn.next == null) return@forEachIndexed
            if(insn.next.type == AbstractInsnNode.LABEL ||
                        insn.type in arrayOf(AbstractInsnNode.JUMP_INSN, AbstractInsnNode.LOOKUPSWITCH_INSN, AbstractInsnNode.TABLESWITCH_INSN)) {
                currentBlock = Block()
                currentBlock.start = index + 1
                currentBlock.end = index + 1
                blocks.add(currentBlock)
            }
        }
    }

    override fun newControlFlowEdge(insnIndex: Int, nextIndex: Int) {
        val block1 = findBlock(insnIndex)
        val block2 = findBlock(nextIndex)
        if(block1 != block2) {
            if(insnIndex + 1 == nextIndex) {
                block1.next = block2
                block2.prev = block1
            } else {
                block1.tree.add(block2)
            }
        }
    }

    private fun findBlock(index: Int): Block {
        return blocks.first { index in it.start until it.end }
    }
}