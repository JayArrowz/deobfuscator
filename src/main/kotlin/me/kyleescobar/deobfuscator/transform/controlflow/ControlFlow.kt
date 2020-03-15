package me.kyleescobar.deobfuscator.transform.controlflow

import me.kyleescobar.deobfuscator.asm.ClassGroup
import me.kyleescobar.deobfuscator.transform.Transformer
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode
import org.tinylog.kotlin.Logger
import java.util.*
import kotlin.collections.AbstractMap

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

class ControlFlow : Transformer {

    private var counter = 0

    override fun transform(group: ClassGroup) {
        group.forEach { c ->
            c.methods.forEach { m ->
                if(m.tryCatchBlocks.isEmpty()) {
                    val disjointTree = DisjointAnalyzer()
                    disjointTree.analyze(c.name, m)
                    counter += disjointTree.blocks.size
                    m.instructions = transformInstructions(m.instructions, disjointTree.blocks)
                }
            }
        }

        Logger.info("Reordered $counter method instructions.")
    }

    private fun transformInstructions(origInsnList: InsnList, blocks: List<Block>): InsnList {
        val instructions = InsnList()
        if(blocks.isEmpty()) return instructions

        val labelMap = LabelMap()
        val stack: Queue<Block> = Collections.asLifoQueue(ArrayDeque())
        stack.add(blocks.first())
        val placed = hashSetOf<Block>()
        while(stack.isNotEmpty()) {
            val b = stack.remove()
            if(b in placed) continue
            placed.add(b)
            b.tree.forEach { stack.add(it.root) }
            b.next?.let { stack.add(it) }
            for(i in b.start until b.end) {
                instructions.add(origInsnList[i].clone(labelMap))
            }
        }
        return instructions
    }

    private class LabelMap : AbstractMap<LabelNode, LabelNode>() {
        private val map = hashMapOf<LabelNode, LabelNode>()
        override val entries: Set<Map.Entry<LabelNode, LabelNode>> get() = map.entries
        override fun get(key: LabelNode): LabelNode = map.getOrPut(key) { LabelNode() }
    }
}