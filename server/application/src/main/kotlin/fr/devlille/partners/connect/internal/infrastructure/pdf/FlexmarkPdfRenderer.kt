package fr.devlille.partners.connect.internal.infrastructure.pdf

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension
import com.vladsch.flexmark.util.data.DataHolder
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.ByteArrayOutputStream

fun renderMarkdownToPdf(markdown: String): ByteArray {
    val options: DataHolder = MutableDataSet().toImmutable()
    val parser = Parser.builder(options).build()
    val document = parser.parse(markdown.trimIndent())
    val renderer = HtmlRenderer.builder(options).build()
    val text = renderer.render(document)
    val stream = ByteArrayOutputStream()
    PdfConverterExtension.exportToPdf(stream, text, "", options)
    return stream.toByteArray()
}
