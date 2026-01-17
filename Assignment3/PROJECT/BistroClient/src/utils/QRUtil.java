package utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;

/**
 * Utility class for generating QR code images for JavaFX.
 * <p>
 * This class uses the ZXing (Zebra Crossing) library to encode a given text
 * into a QR code ({@link BarcodeFormat#QR_CODE}) and convert the result into a
 * JavaFX {@link Image} that can be displayed in the client UI.
 * </p>
 *
 * <p><b>Library:</b> ZXing – Open Source Barcode Image Processing Library</p>
 * <p><b>License:</b> Apache License 2.0</p>
 */
public class QRUtil {

    private QRUtil() {
        // Utility class; prevent instantiation.
    }

    /**
     * Generates a scannable QR code image from the given text.
     * <p>
     * The QR code is encoded using {@link QRCodeWriter}, rendered to a
     * {@link BufferedImage} via {@link MatrixToImageWriter}, and then converted
     * to a JavaFX {@link Image} using {@link SwingFXUtils}.
     * </p>
     *
     * @param text the content to encode inside the QR code
     * @return a JavaFX {@link Image} containing the generated QR code,
     *         or {@code null} if QR generation fails
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
