package me.kyleescobar.deobfuscator.transform

import me.kyleescobar.deobfuscator.asm.ClassGroup
import me.kyleescobar.deobfuscator.asm.hierarchy.ClassHierarchy
import me.kyleescobar.deobfuscator.asm.hierarchy.HierarchyVisitor
import me.kyleescobar.deobfuscator.asm.mapping.BetterClassRemapper
import me.kyleescobar.deobfuscator.asm.mapping.BetterRemapper
import me.kyleescobar.deobfuscator.asm.mapping.ClassMapping
import org.objectweb.asm.tree.ClassNode
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

class Renamer : Transformer {

    private lateinit var hierarchy: HashMap<String, ClassHierarchy>
    private val mappings = hashMapOf<String, ClassMapping>()

    private var classCounter = 0
    private var methodCounter = 0
    private var fieldCounter = 0

    override fun transform(group: ClassGroup) {
        this.buildHierarchy(group)
        this.generateMapping(group)
        this.applyMapping(group)
        Logger.info("Renamed: [classes=$classCounter, methods=$methodCounter, fields=$fieldCounter].")
    }

    private fun buildHierarchy(group: ClassGroup) {
        val hierarchyVisitor = HierarchyVisitor()
        group.forEach { c ->
            c.accept(hierarchyVisitor)
        }

        hierarchy = hierarchyVisitor.hierarchy
    }

    private fun generateMapping(group: ClassGroup) {
        group.forEach { c ->
            if(c.name.length <= 2) {
                mappings[c.name] = ClassMapping(
                    classMapping = "class${++classCounter}",
                    methodMappings = hashMapOf(),
                    fieldMapping = hashMapOf()
                )
            }
        }

        group.forEach classLoop@ { c ->
            c.fields.forEach fieldLoop@ { f ->
                if(f.name.length <= 2 ) {
                    val mapping = mappings[c.name] ?: return@fieldLoop
                    mapping.fieldMapping[f.name to f.desc] = "field${++fieldCounter}"
                }
            }

            c.methods.forEach methodLoop@ { m ->
                if(m.name.length <= 2) {
                    val mapping = mappings[c.name] ?: return@methodLoop
                    mapping.methodMappings[m.name to m.desc] = "method${++methodCounter}"
                }
            }
        }
    }

    private fun applyMapping(group: ClassGroup) {
        val mapper = BetterRemapper(mappings, hierarchy)
        group.forEachIndexed { index, c ->
            val newNode = ClassNode()
            c.accept(BetterClassRemapper(newNode, mapper))
            group[index] = newNode
        }
    }
}