package com.jetbrains.packagesearch.plugin.ui.bridge

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.ResourceLoader
import com.jetbrains.packagesearch.plugin.core.data.PackageSearchModule
import org.jetbrains.jewel.foundation.tree.TreeGeneratorScope
import org.jetbrains.jewel.foundation.tree.buildTree
import java.awt.Desktop
import java.io.InputStream
import java.net.URI
import java.util.jar.JarFile
import javax.swing.UIDefaults
import javax.swing.UIManager

fun java.awt.Color.toComposeColor(): Color {
    return Color(red, green, blue, alpha)
}

fun UIDefaults.getComposeColor(key: String): Color? {
    return getColor(key)?.toComposeColor()
}

fun UIDefaults.getComposeColorOrUnspecified(key: String): Color {
    return getColor(key)?.toComposeColor().let {
        println("No color in LAF for $key, fallback to Color.Unspecified")
        Color.Unspecified
    }
}


fun List<PackageSearchModule>.asTree() =
    buildTree {
        groupBy { it.identity.group }
            .values
            .forEach {
                val sortedItems = it.sortedBy { it.identity.path }
                val roots = sortedItems.filter { it.identity.path == ":" }.toSet()
                roots.forEach { addNodes(sortedItems - roots, it, true) }
            }
    }

fun TreeGeneratorScope<PackageSearchModule>.addNodes(
    sortedItems: List<PackageSearchModule>,
    currentData: PackageSearchModule,
    isRoot: Boolean = false,
) {
    val children = sortedItems
        .filter {
            val toRemove = buildString {
                append(currentData.identity.path)
                if (!isRoot) append(":")
            }
            it.identity.path.removePrefix(toRemove).run {
                isNotEmpty() && !contains(":")
            }
        }
    if (children.isNotEmpty()) {
        addNode(currentData, id = currentData.identity) {
            children.forEach { addNodes(sortedItems - children, it) }
        }
    } else {
        addLeaf(currentData, id = currentData.identity)
    }
}

fun openLinkInBrowser(url: String) {
    Desktop.getDesktop()
        ?.takeIf { it.isSupported(Desktop.Action.BROWSE) }
        ?.browse(URI(url))
}


fun pickComposeColorFromLaf(key: String) =
    UIManager.getLookAndFeelDefaults().getComposeColor(key) ?: Color.Unspecified


fun isLightTheme(): Boolean {
    val laf = UIManager.getLookAndFeelDefaults()
    val brightness = laf.getComposeColor("ToolWindow.background")?.getBrightness() ?: 200f
    return brightness < 128
}

private fun Color.getBrightness() = (red * 299 + green * 587 + blue * 114) / 1000

fun getJarPath(klass: Class<*>): String? {
    val className = klass.name.replace('.', '/') + ".class"
    val classPath = klass.classLoader.getResource(className)?.toString() ?: return null
    if (!classPath.startsWith("jar")) {
        // Class not from a JAR
        return null
    }
    return classPath.substringBefore("!").removePrefix("jar:file:")
}

fun extractFileFromJar(jarPath: String, filePath: String) =
    JarFile(jarPath).use { jar ->
        jar.getEntry(filePath)?.let { entry ->
            jar.getInputStream(entry).use { it.readBytes().inputStream() }
        }
    }

inline fun <reified T> getJarPath(): String? = getJarPath(T::class.java)

class RawJarResourceLoader(private val jars: List<String>) : ResourceLoader {
    override fun load(resourcePath: String): InputStream =
        jars.firstNotNullOfOrNull { extractFileFromJar(it, resourcePath) }
            ?: error("Resource $resourcePath not found in jars:\n ${jars.joinToString("\n") { "- $it" }}")
}

class JarPathBuilder {

    private val jars = mutableSetOf<String>()

    fun add(clazz: Class<*>) {
        getJarPath(clazz)?.let { jars.add(it) }
    }

    fun getJarPaths() = jars.toList()
}

inline fun <reified T> JarPathBuilder.add() = add(T::class.java)

fun buildJarPaths(builder: JarPathBuilder.() -> Unit) =
    JarPathBuilder().apply(builder).getJarPaths()

fun RawJarResourceLoader(builder: JarPathBuilder.() -> Unit) =
    RawJarResourceLoader(buildJarPaths(builder))




