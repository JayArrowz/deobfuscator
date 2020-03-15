package me.kyleescobar.deobfuscator.asm.mapping

import me.kyleescobar.deobfuscator.asm.hierarchy.ClassHierarchy
import org.objectweb.asm.commons.Remapper

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

class BetterRemapper(
    val mappings: HashMap<String, ClassMapping>,
    val hierarchy: HashMap<String, ClassHierarchy>
) : Remapper() {

    override fun map(internalName: String): String {
        return (mappings[internalName]?.classMapping) ?: internalName
    }

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        val h = hierarchy[owner] ?: return name
        if(h.superName != null && h.superName.length <= 2) mapMethodName(h.superName, name, descriptor).let { return it }
        if(h.interfaces != null && h.interfaces.any { it.length <= 2 }) h.interfaces.forEach { interf ->
            mapMethodName(interf, name, descriptor).let { return it }
        }
        mappings[owner]?.let { it.methodMappings[name to descriptor] }?.let { return it }
        return name
    }

    override fun mapFieldName(owner: String, name: String, descriptor: String): String {
        mappings[owner]?.let { it.fieldMapping[name to descriptor] }?.let { return it }
        hierarchy[owner]?.superName?.let { return mapFieldName(it, name, descriptor) }
        return name
    }
}