@file:Suppress("DEPRECATION")

package me.kyleescobar.deobfuscator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import org.tinylog.kotlin.Logger
import java.applet.Applet
import java.applet.AppletContext
import java.applet.AppletStub
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import javax.swing.JFrame

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

class TestClient : CliktCommand(
    name = "testclient",
    help = "Runs a test JAGEX client from an input JAR file.",
    printHelpOnEmptyArgs = true
) {

    private val input: File by argument(name = "Input File", help = "The path to the input JAR file.").file(mustExist = true, canBeDir = false)

    override fun run() {
        Logger.info("Starting test client")

        val frame = JFrame()
        frame.layout = GridLayout(1, 0)

        val applet = start()
        frame.add(applet)
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }

    private fun start(): Applet {
        val params = crawl()
        val classLoader = URLClassLoader(arrayOf(input.toURI().toURL()))
        val main = params["initial_class"]!!.replace(".class", "")
        val applet = classLoader.loadClass(main).newInstance() as Applet
        applet.background = Color.BLACK
        applet.preferredSize = Dimension(Integer.parseInt(params["applet_minwidth"]), Integer.parseInt(params["applet_minheight"]))
        applet.size = applet.preferredSize
        applet.layout = null
        applet.setStub(createAppletStub(params, applet))
        applet.isVisible = true
        applet.init()
        return applet
    }

    private fun crawl(): HashMap<String, String> {
        val CONFIG_URL = "https://oldschool6.runescape.com/jav_config.ws"
        val lines = URL(CONFIG_URL).readText().split("\n")
        val params = hashMapOf<String, String>()
        lines.forEach {
            var line = it
            if(line.startsWith("param=")) {
                line = line.substring(6)
            }
            val idx = line.indexOf("=")
            if(idx >= 0) {
                params[line.substring(0, idx)] = line.substring(idx + 1)
            }
        }

        return params
    }

    private fun createAppletStub(params: Map<String, String>, applet: Applet): AppletStub {
        return object : AppletStub {
            override fun isActive(): Boolean = true
            override fun getDocumentBase(): URL = URL(params["codebase"])
            override fun getCodeBase(): URL = URL(params["codebase"])
            override fun getParameter(p0: String): String? = params[p0]
            override fun appletResize(p0: Int, p1: Int) { applet.size = Dimension(p0, p1) }
            override fun getAppletContext(): AppletContext? = null
        }
    }
}