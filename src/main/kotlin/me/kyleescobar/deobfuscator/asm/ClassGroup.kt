package me.kyleescobar.deobfuscator.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

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

typealias ClassGroup = MutableList<ClassNode>

fun emptyClassGroup(): ClassGroup = mutableListOf()

fun ClassGroup.loadJar(file: File) {
    JarFile(file).use { jar ->
        val entries = jar.entries()
        while(entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if(entry.name.endsWith(".class")) {
                val node = ClassNode()
                val reader = ClassReader(jar.getInputStream(entry))
                reader.accept(node, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
                this.add(node)
            }
        }
        jar.close()
    }
}

fun ClassGroup.export(file: File) {
    val out = JarOutputStream(FileOutputStream(file))
    this.forEach { c ->
        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
        c.accept(writer)
        out.putNextEntry(JarEntry(c.name + ".class"))
        out.write(writer.toByteArray())
        out.closeEntry()
    }
    out.close()
}