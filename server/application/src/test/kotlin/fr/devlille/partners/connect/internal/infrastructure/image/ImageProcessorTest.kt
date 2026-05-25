package fr.devlille.partners.connect.internal.infrastructure.image

import fr.devlille.partners.connect.internal.infrastructure.api.PayloadTooLargeException
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImageProcessorTest {
    @Test
    fun `resizes a PNG to all requested widths`() {
        val bytes = pngBytes(width = 200, height = 100, color = Color.RED)

        val resized = ImageProcessor.resizeImageToWidths(bytes, listOf(100, 50, 25))

        assertEquals(setOf(100, 50, 25), resized.keys)
        resized.forEach { (width, png) ->
            val decoded = ImageIO.read(png.inputStream())
            assertEquals(width, decoded.width)
        }
    }

    @Test
    fun `throws PayloadTooLargeException when declared dimensions exceed cap`() {
        val bytes = craftPngHeaderOnly(width = 20_000, height = 20_000)

        assertFailsWith<PayloadTooLargeException> {
            ImageProcessor.resizeImageToWidths(bytes, listOf(1000))
        }
    }

    private fun pngBytes(width: Int, height: Int, color: Color): ByteArray {
        val src = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = src.createGraphics()
        g.color = color
        g.fillRect(0, 0, width, height)
        g.dispose()
        return ByteArrayOutputStream().use { out ->
            ImageIO.write(src, "png", out)
            out.toByteArray()
        }
    }

    // A minimal valid PNG signature + IHDR chunk with crafted dimensions. Enough for
    // ImageReader.getWidth/getHeight to succeed without allocating a raster.
    private fun craftPngHeaderOnly(width: Int, height: Int): ByteArray {
        val signature = byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
        )
        val ihdrType = "IHDR".toByteArray()
        val ihdrData = ByteArrayOutputStream().apply {
            writeIntBe(width)
            writeIntBe(height)
            write(8)
            write(6)
            write(0)
            write(0)
            write(0)
        }.toByteArray()
        val crc = CRC32().apply {
            update(ihdrType)
            update(ihdrData)
        }.value
        return ByteArrayOutputStream().apply {
            write(signature)
            writeIntBe(ihdrData.size)
            write(ihdrType)
            write(ihdrData)
            writeIntBe(crc.toInt())
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeIntBe(value: Int) {
        write((value ushr 24) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }
}
