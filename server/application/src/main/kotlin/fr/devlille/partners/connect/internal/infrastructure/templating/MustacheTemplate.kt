package fr.devlille.partners.connect.internal.infrastructure.templating

import com.github.mustachejava.DefaultMustacheFactory
import java.io.StringReader
import java.io.StringWriter

inline fun <reified T> templating(template: String, scope: T): String {
    val mustache = DefaultMustacheFactory().compile(StringReader(template), "template")
    val writer = StringWriter()
    mustache.execute(writer, scope).flush()
    return writer.buffer.toString()
}
