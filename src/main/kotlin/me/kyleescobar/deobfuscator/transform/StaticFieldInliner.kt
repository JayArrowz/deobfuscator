package me.kyleescobar.deobfuscator.transform

import me.kyleescobar.deobfuscator.asm.ClassGroup
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.tinylog.kotlin.Logger
import java.lang.reflect.Modifier

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
 * Inlines static fields which are only used in single methods.
 */
class StaticFieldInliner : Transformer {

    private var counter = 0

    override fun transform(group: ClassGroup) {
        val resolver = Resolver(group)
        group.forEach { c ->
            c.methods.forEach { m ->
                m.instructions.iterator().forEach { insn ->
                    if(insn is FieldInsnNode) {
                        val op = insn.opcode
                        val prevOwner = insn.owner
                        insn.owner = resolver.getParent(insn.owner, insn.name, insn.desc, op == Opcodes.GETSTATIC || op == Opcodes.PUTSTATIC)
                        val newOwner = insn.owner
                        if(prevOwner != newOwner) counter++
                    }
                }
            }
        }

        Logger.info("Inlined $counter fields.")
    }

    private class Resolver(group: ClassGroup) {
        private val namedGroup = group.associateBy { it.name }

        fun getParent(owner: String, name: String, desc: String, isStatic: Boolean): String {
            var cn = namedGroup[owner] ?: return owner
            while(true) {
                if(cn.hasDeclaredField(name, desc, isStatic)) {
                    return cn.name
                }
                val superName = cn.superName
                cn = namedGroup[superName] ?: return superName
            }
        }

        private fun ClassNode.hasDeclaredField(name: String, desc: String, isStatic: Boolean): Boolean {
            return fields.any {
                it.name == name && it.desc == desc && Modifier.isStatic(it.access) == isStatic
            }
        }
    }
}