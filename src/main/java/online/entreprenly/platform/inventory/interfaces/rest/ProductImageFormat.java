package online.entreprenly.platform.inventory.interfaces.rest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Detects the format of an uploaded product photo from its bytes (never from the client-declared
 * content type), accepting only JPG, PNG and WebP up to {@link #MAX_BYTES}.
 */
public final class ProductImageFormat {
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    private ProductImageFormat() {
    }

    /**
     * Returns the MIME type of the image.
     *
     * @param data the uploaded bytes
     * @return {@code image/jpeg}, {@code image/png} or {@code image/webp}
     * @throws IllegalArgumentException when the file is too large or not a supported image
     */
    public static String detect(byte[] data) {
        if (data.length > MAX_BYTES) throw new IllegalArgumentException("The photo must be 5 MB or less.");
        if (data.length >= 24 && prefix(data, 0, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})
                && ascii(data, 12, "IHDR")) return "image/png";
        if (data.length >= 5 && prefix(data, 0, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff})
                && prefix(data, data.length - 2, new byte[]{(byte) 0xff, (byte) 0xd9})) return "image/jpeg";
        if (data.length >= 20 && ascii(data, 0, "RIFF") && ascii(data, 8, "WEBP")
                && ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() == data.length - 8
                && (ascii(data, 12, "VP8 ") || ascii(data, 12, "VP8L") || ascii(data, 12, "VP8X"))) {
            return "image/webp";
        }
        throw new IllegalArgumentException("Choose a valid JPG, PNG or WebP image.");
    }

    private static boolean ascii(byte[] data, int offset, String value) {
        return prefix(data, offset, value.getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean prefix(byte[] data, int offset, byte[] value) {
        for (int i = 0; i < value.length; i++) {
            if (data[offset + i] != value[i]) return false;
        }
        return true;
    }
}
