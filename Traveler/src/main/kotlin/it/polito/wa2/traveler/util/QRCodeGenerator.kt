package it.polito.wa2.traveler.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.client.j2se.MatrixToImageConfig
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path


object QRCodeGenerator {
    @Throws(WriterException::class, IOException::class)
    fun generateQRCodeImage(text: String?, width: Int, height: Int, filePath: String?) {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
        val path: Path = FileSystems.getDefault().getPath(filePath)
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path)
    }

    @Throws(WriterException::class, IOException::class)
    fun getQRCodeImage(text: String?, width: Int, height: Int): ByteArray {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
        val pngOutputStream = ByteArrayOutputStream()
        val con = MatrixToImageConfig(-0xfffffe, -0x3fbf)
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream, con)
        return pngOutputStream.toByteArray()
    }

    @Throws(Exception::class)
    fun generateQRCodeImage(barcodeText: String?): BufferedImage? {
        val barcodeWriter = QRCodeWriter()
        val bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 200, 200)
        return MatrixToImageWriter.toBufferedImage(bitMatrix)
    }
}
