package me.kyleescobar.deobfuscator.transform

import com.google.common.collect.MultimapBuilder
import me.kyleescobar.deobfuscator.asm.ClassGroup
import me.kyleescobar.deobfuscator.asm.isConstantIntProducer
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.tinylog.kotlin.Logger
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

class OpaquePredicateArgumentRemover : Transformer {

    override fun transform(group: ClassGroup) {
        val namedGroup = group.associateBy { it.name }
        val methodDescriptorChanged = TreeMap<String, String>()
        var insnCounter = 0
        val topMethods = hashSetOf<String>()

        group.forEach { c ->
            val supers = supers(c, classNames = namedGroup)
            c.methods.forEach { m ->
                if(supers.none { it.methods.any { it.name == m.name && it.desc == m.desc }})
                    topMethods.add("${c.name}.${m.name}${m.desc}")
            }
        }

        val implementationMulti = MultimapBuilder.hashKeys().arrayListValues().build<String, ClassMethod>()
        val implementations = implementationMulti.asMap()
        group.forEach { c ->
            c.methods.forEach methodLoop@ { m ->
                val s = overrides(c.name, m.name + m.desc, topMethods, namedGroup) ?: return@methodLoop
                implementationMulti.put(s, ClassMethod(c, m))
            }
        }

        val itr = implementations.iterator()
        for(e in itr) {
            if(e.value.any { !hasUnusedLastParamInt(it.m) }) {
                itr.remove()
            }
        }

        group.forEach { c ->
            c.methods.forEach { m ->
                val insnList = m.instructions
                for(insn in insnList) {
                    if(insn !is MethodInsnNode) continue
                    val s = overrides(insn.owner, insn.name + insn.desc, implementations.keys, namedGroup) ?: continue
                    if(!insn.previous.isConstantIntProducer) {
                        implementations.remove(s)
                    }
                }
            }
        }

        implementationMulti.values().forEach { (c, m) ->
            val oldDesc = m.desc
            val newDesc = dropLastArg(oldDesc)
            m.desc = newDesc
            methodDescriptorChanged["${c.name}.${m.name}$newDesc"] = oldDesc
        }

        group.forEach { c ->
            c.methods.forEach { m ->
                val insnList = m.instructions
                for(insn in insnList) {
                    if(insn !is MethodInsnNode) continue
                    if(overrides(insn.owner, insn.name + insn.desc, implementations.keys, namedGroup) != null) {
                        insn.desc = dropLastArg(insn.desc)
                        val prev = insn.previous
                        check(prev.isConstantIntProducer)
                        insnList.remove(prev)
                        insnCounter++
                    }
                }
            }
        }

        Logger.info("Removed $insnCounter instruction INT arguments.")
    }

    private fun overrides(owner: String, nameDesc: String, methods: Set<String>, classNames: Map<String, ClassNode>): String? {
        val s = "$owner.$nameDesc"
        if (s in methods) return s
        if (nameDesc.startsWith("<init>")) return null
        val classNode = classNames[owner] ?: return null
        for (sup in supers(classNode, classNames)) {
            return overrides(sup.name, nameDesc, methods, classNames) ?: continue
        }
        return null
    }

    private fun supers(c: ClassNode, classNames: Map<String, ClassNode>): Collection<ClassNode> {
        return c.interfaces.plus(c.superName).mapNotNull { classNames[it] }.flatMap { supers(it, classNames).plus(it) }
    }

    private fun hasUnusedLastParamInt(m: MethodNode): Boolean {
        val argTypes = Type.getArgumentTypes(m.desc)
        if (argTypes.isEmpty()) return false
        val lastArg = argTypes.last()
        if (lastArg != Type.BYTE_TYPE && lastArg != Type.SHORT_TYPE && lastArg != Type.INT_TYPE) return false
        if (Modifier.isAbstract(m.access)) return true
        val lastParamLocalIndex = (if (Modifier.isStatic(m.access)) -1 else 0) + (Type.getArgumentsAndReturnSizes(m.desc) shr 2) - 1
        for (insn in m.instructions) {
            if (insn !is VarInsnNode) continue
            if (insn.`var` == lastParamLocalIndex) return false
        }
        return true
    }

    private fun dropLastArg(desc: String): String {
        val type = Type.getMethodType(desc)
        return Type.getMethodDescriptor(type.returnType, *type.argumentTypes.copyOf(type.argumentTypes.size - 1))
    }

    private data class ClassMethod(val c: ClassNode, val m: MethodNode)
}