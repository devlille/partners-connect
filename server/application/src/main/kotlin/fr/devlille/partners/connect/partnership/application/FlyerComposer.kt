package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.sponsoring.domain.FlyerZone
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object FlyerComposer {
    private const val ZONE_MARGIN_PX = 20

    /**
     * Composes the partner's logo into the template's configured zone and returns JPG bytes.
     * The logo is scaled (preserving aspect ratio) to fit inside the zone minus a 20px margin
     * on each side, and centred within the zone. Template dimensions are preserved.
     */
    fun compose(templatePng: ByteArray, logo: ByteArray, zone: FlyerZone): ByteArray {
        val templateImg = ImageIO.read(ByteArrayInputStream(templatePng))
            ?: error("Template bytes are not a readable image")
        val logoImg = ImageIO.read(ByteArrayInputStream(logo))
            ?: error("Logo bytes are not a readable image")

        val availableWidth = zone.width - (ZONE_MARGIN_PX * 2)
        val availableHeight = zone.height - (ZONE_MARGIN_PX * 2)
        val resized = scaleToFit(logoImg, availableWidth, availableHeight)

        val xPosition = zone.x + (zone.width - resized.width) / 2
        val yPosition = zone.y + (zone.height - resized.height) / 2

        val output = BufferedImage(templateImg.width, templateImg.height, BufferedImage.TYPE_INT_RGB)
        val g = output.createGraphics()
        g.drawImage(templateImg, 0, 0, null)
        g.drawImage(resized, xPosition, yPosition, null)
        g.dispose()

        val out = ByteArrayOutputStream()
        ImageIO.write(output, "jpg", out)
        return out.toByteArray()
    }

    private fun scaleToFit(logo: BufferedImage, availableWidth: Int, availableHeight: Int): BufferedImage {
        val aspect = logo.width.toDouble() / logo.height.toDouble()
        var targetWidth = availableWidth
        var targetHeight = (targetWidth / aspect).toInt()
        if (targetHeight > availableHeight) {
            targetHeight = availableHeight
            targetWidth = (targetHeight * aspect).toInt()
        }
        return Scalr.resize(logo, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, targetWidth, targetHeight)
    }
}
