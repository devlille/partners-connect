package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.sponsoring.domain.FlyerZone
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlyerComposerTest {
    private fun pngBytes(width: Int, height: Int, fill: Color = Color.WHITE): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = fill
        g.fillRect(0, 0, width, height)
        g.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "png", out)
        return out.toByteArray()
    }

    @Test
    fun `compose returns a readable JPG with same dimensions as the template`() {
        val template = pngBytes(1200, 800)
        val logo = pngBytes(400, 400, Color.RED)
        val zone = FlyerZone(x = 100, y = 200, width = 800, height = 500)

        val jpgBytes = FlyerComposer.compose(template, logo, zone)
        val rendered = ImageIO.read(ByteArrayInputStream(jpgBytes))

        assertEquals(1200, rendered.width)
        assertEquals(800, rendered.height)
    }

    @Test
    fun `compose draws the logo only within the configured zone respecting a 20px margin`() {
        val template = pngBytes(1200, 800, Color.WHITE)
        val logo = pngBytes(400, 400, Color.RED)
        val zone = FlyerZone(x = 100, y = 200, width = 800, height = 500)

        val jpgBytes = FlyerComposer.compose(template, logo, zone)
        val rendered = ImageIO.read(ByteArrayInputStream(jpgBytes))

        val outsidePixel = Color(rendered.getRGB(zone.x - 1, zone.y - 1))
        assertEquals(255, outsidePixel.red)
        assertEquals(255, outsidePixel.green)
        assertEquals(255, outsidePixel.blue)

        val marginPixel = Color(rendered.getRGB(zone.x + 5, zone.y + 5))
        assertEquals(255, marginPixel.red)
        assertEquals(255, marginPixel.green)
        assertEquals(255, marginPixel.blue)

        val centrePixel = Color(rendered.getRGB(zone.x + zone.width / 2, zone.y + zone.height / 2))
        assertTrue(centrePixel.red > 200, "Centre pixel should be predominantly red, got $centrePixel")
        assertTrue(centrePixel.green < 80, "Centre pixel green channel should be low, got $centrePixel")
    }

    @Test
    fun `compose preserves logo aspect ratio when it is wider than tall`() {
        val template = pngBytes(1200, 800)
        val logo = pngBytes(800, 200, Color.RED)
        val zone = FlyerZone(x = 100, y = 200, width = 800, height = 500)

        val jpgBytes = FlyerComposer.compose(template, logo, zone)
        val rendered = ImageIO.read(ByteArrayInputStream(jpgBytes))

        val topOfZone = Color(rendered.getRGB(zone.x + zone.width / 2, zone.y + 5))
        assertEquals(255, topOfZone.red)
        assertEquals(255, topOfZone.green)
    }
}
