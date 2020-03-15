package me.kyleescobar.deobfuscator.transform

import me.kyleescobar.deobfuscator.asm.ClassGroup
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.tinylog.kotlin.Logger

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
 * Removes the GOTO instruction opcodes.
 */
class GotoRemover : Transformer {

    private var counter = 0

    override fun transform(group: ClassGroup) {
        group.flatMap {  it.methods }.forEach { m ->
            val instructions = m.instructions.iterator()
            while(instructions.hasNext()) {
                val insn0 = instructions.next()
                if(insn0.opcode != Opcodes.GOTO) continue
                insn0 as JumpInsnNode

                val insn1 = insn0.next
                if(insn1 == null || insn1 !is LabelNode) continue
                if(insn0.label == insn1) {
                    instructions.remove()
                    counter++
                }
            }
        }

        Logger.info("Removed $counter GOTO instructions.")
    }
}