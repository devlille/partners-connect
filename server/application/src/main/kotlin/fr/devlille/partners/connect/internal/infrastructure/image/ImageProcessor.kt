package fr.devlille.partners.connect.internal.infrastructure.image

import fr.devlille.partners.connect.internal.infrastructure.api.PayloadTooLargeException
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
    private const val MAX_IMAGE_DIMENSION = 10_000

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

    fun resizeImageToWidths(bytes: ByteArray, widths: List<Int>): Map<Int, ByteArray> {
        require(widths.isNotEmpty()) { "widths must not be empty" }
        val maxOut = widths.max()
        return ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { iis ->
            val reader = ImageIO.getImageReaders(iis).asSequence().firstOrNull()
                ?: error("No image reader available for uploaded bytes")
            try {
                reader.input = iis
                val srcWidth = reader.getWidth(0)
                val srcHeight = reader.getHeight(0)
                if (srcWidth > MAX_IMAGE_DIMENSION || srcHeight > MAX_IMAGE_DIMENSION) {
                    throw PayloadTooLargeException(
                        "Image dimensions ${srcWidth}x$srcHeight exceed maximum of " +
                            "${MAX_IMAGE_DIMENSION}x$MAX_IMAGE_DIMENSION",
                    )
                }
                val subsample = (minOf(srcWidth, srcHeight) / (maxOut * 2)).coerceAtLeast(1)
                val param = reader.defaultReadParam.apply {
                    setSourceSubsampling(subsample, subsample, 0, 0)
                }
                val decoded = reader.read(0, param)
                widths.associateWith { width ->
                    val resized = Scalr.resize(decoded, width)
                    ByteArrayOutputStream().use { out ->
                        ImageIO.write(resized, "png", out)
                        out.toByteArray()
                    }
                }
            } finally {
                reader.dispose()
            }
        }
    }
}
