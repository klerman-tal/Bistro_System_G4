/**
 * QR code generation utility.
 *
 * This class uses the ZXing (Zebra Crossing) open-source library
 * to generate a scannable QR code image from a given text.
 *
 * The QR generation logic is based on official ZXing examples
 * and adapted for use within a JavaFX client application.
 *
 * Library:
 * ZXing – Open Source Barcode Image Processing Library
 * License: Apache License 2.0
 * Source: https://github.com/zxing/zxing
 */

package utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;

public class QRUtil {

    /**
     * Generates a scannable QR code image from the given text.
     *
     * This method uses the ZXing library to encode the provided text
     * into a standard-compliant QR code and converts it into a JavaFX Image
     * for display within the client application.
     *
     * @param text the content to encode inside the QR code
     * @return a JavaFX Image containing the generated QR code,
     *         or null if QR generation fails
     */
    public static Image generateQR(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    220,
                    220
            );

            BufferedImage bufferedImage =
                    MatrixToImageWriter.toBufferedImage(matrix);

            return SwingFXUtils.toFXImage(bufferedImage, null);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
