package fr.devlille.partners.connect.internal.infrastructure.image

import kotlinx.io.IOException
import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.imgscalr.Scalr
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ImageProcessor {
    fun resizeSvg(bytes: ByteArray, width: Int): ByteArray? {
        val input = TranscoderInput(ByteArrayInputStream(bytes))
        val transcoder = PNGTranscoder().apply {
            addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, width.toFloat())
        }
        return try {
            ByteArrayOutputStream().use {
                val output = TranscoderOutput(it)
                transcoder.transcode(input, output)
                it.toByteArray()
            }
        } catch (_: SAXException) {
            null
        }
    }

    fun resizeImage(bytes: ByteArray, width: Int): ByteArray? {
        val resized = Scalr.resize(ImageIO.read(bytes.inputStream()), width)
        try {
            val stream = ByteArrayOutputStream()
            ImageIO.write(resized, "png", stream)
            return stream.toByteArray()
        } catch (_: IOException) {
            return null
        }
    }
}
