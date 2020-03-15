package me.kyleescobar.deobfuscator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import com.google.common.base.Stopwatch
import me.kyleescobar.deobfuscator.asm.emptyClassGroup
import me.kyleescobar.deobfuscator.asm.export
import me.kyleescobar.deobfuscator.asm.loadJar
import me.kyleescobar.deobfuscator.transform.Renamer
import me.kyleescobar.deobfuscator.transform.Transformer
import me.kyleescobar.deobfuscator.transform.controlflow.ControlFlow
import org.tinylog.kotlin.Logger
import java.io.File
import java.util.concurrent.TimeUnit

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

class Deobfuscator : CliktCommand(
    name = "deob",
    help = "Reformats an obfuscated OSRS client to make it more readable.",
    printHelpOnEmptyArgs = true
) {

    private val input: File by argument(name = "Input File", help = "The path to in input JAR file.").file(mustExist = true, canBeDir = false, canBeFile = true)
    private val output: File by argument(name = "Output File", help = "The path of the output JAR file.").file(canBeDir = false, canBeFile = true)

    private val group = emptyClassGroup()

    override fun run() {
        Logger.info("Starting deobfuscator.")
        this.loadJar()
        this.runTransformers()
        this.exportJar()
    }

    private fun loadJar() {
        Logger.info("Loading input JAR.")
        group.loadJar(input)
        Logger.info("Finished loading input JAR file. Found ${group.size} classes.")
    }

    private fun runTransformers() {
        Logger.info("Running class transformers.")

        /**
         * Run each transformer in order.
         */
        run { Renamer() }
        run { ControlFlow() }

        Logger.info("Finished running class transformers.")
    }

    private fun exportJar() {
        Logger.info("Exporting transformed classes to JAR file.")
        group.export(output)
        Logger.info("Finished exporting to JAR file.")
    }

    private fun run(transformer: () -> Transformer) {
        val stopwatch = Stopwatch.createStarted()
        Logger.info("Running transformer '${transformer().javaClass.simpleName}'.")
        transformer().transform(group)
        Logger.info("Finished transformer '${transformer().javaClass.simpleName}' in ${stopwatch.stop().elapsed(TimeUnit.SECONDS)} seconds.")
    }
}