package online.entreprenly.platform;

import online.entreprenly.platform.profile.interfaces.rest.AvatarFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileAv2RulesTest {
    @Test
    void rejectsUnsupportedDataRegardlessOfClientContentType() {
        assertThrows(IllegalArgumentException.class, () -> AvatarFormat.detect("<svg>".getBytes()));
        assertThrows(IllegalArgumentException.class, () -> AvatarFormat.detect(new byte[5 * 1024 * 1024 + 1]));
    }

    @Test
    void recognizesPngSignature() throws java.io.IOException {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1, 1, 2), "png", bytes);
        assertEquals("image/png", AvatarFormat.detect(bytes.toByteArray()));
    }
}
