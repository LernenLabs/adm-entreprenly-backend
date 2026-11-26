package online.entreprenly.platform;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import javax.imageio.ImageIO;
import online.entreprenly.platform.inventory.infrastructure.persistence.jpa.entities.UnitProductPersistenceEntity;
import online.entreprenly.platform.inventory.infrastructure.persistence.jpa.repositories.UnitProductPersistenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.profiles.active=av2",
    "spring.datasource.url=jdbc:h2:mem:profile_acceptance;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "profile.verification.inbox=target/profile-acceptance-inbox"
})
@AutoConfigureMockMvc
class ProfileAcceptanceTest {
    private static final String AUTH = "/api/v1/authentication";
    private static final String EMAIL_CHANGE = "/api/v1/users/me/email-change-requests";
    private static final String PASSWORD = "OriginalPass123!";
    private static final String NEW_PASSWORD = "ReplacementPass456!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate database;
    @Autowired UnitProductPersistenceRepository products;

    @Test
    void expiredEmailLinkCanBeReplacedAndChangingLoginPreservesMerchantData() throws Exception {
        String oldEmail = "previous-profile@example.com";
        String newEmail = "new-profile@example.com";
        register(oldEmail);
        var session = signIn(oldEmail, PASSWORD);
        var product = new UnitProductPersistenceEntity();
        product.setOwnerEmail(oldEmail);
        product.setName("Prueba de inventario");
        product.setPrice(15.0);
        products.save(product);

        Path inbox = Path.of("target/profile-acceptance-inbox/email-" + session.id() + ".txt");
        try {
            requestEmail(session, newEmail);
            String expired = confirmationPath(inbox);
            database.update("update verification_challenges set issued_at = ? where id = ?",
                    Timestamp.from(Instant.now().minusSeconds(700)), "email:" + session.id());
            mvc.perform(get(expired)).andExpect(status().isGone());
            requestEmail(session, newEmail);
            String active = confirmationPath(inbox);
            mvc.perform(get(active)).andExpect(status().isOk());
            mvc.perform(post(active)).andExpect(status().isOk());
            assertNotNull(database.queryForObject("select email_verified_at from users where id = ?",
                    Timestamp.class, session.id()));
            mvc.perform(post(active)).andExpect(status().isGone());
            mvc.perform(json(post(AUTH + "/sign-in"), credentials(oldEmail, PASSWORD)))
                    .andExpect(status().is4xxClientError());
            var newSession = signIn(newEmail, PASSWORD);
            mvc.perform(get("/api/v1/inventory-unit-products").header("Authorization", newSession.bearer()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value(product.getName()));
            mvc.perform(json(post(AUTH + "/sign-up"), credentials(oldEmail, PASSWORD)))
                    .andExpect(status().isConflict());
        } finally {
            Files.deleteIfExists(inbox);
        }
    }

    @Test
    void passwordChangeRevokesOtherSessionsAndKeepsTheOneThatChangedIt() throws Exception {
        String email = "profile-password@example.com";
        register(email);
        var current = signIn(email, PASSWORD);
        var other = signIn(email, PASSWORD);
        String url = "/api/v1/users/me/password";
        mvc.perform(json(put(url).header("Authorization", current.bearer()),
                "{\"currentPassword\":\"IncorrectPass123!\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(json(put(url).header("Authorization", current.bearer()),
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put(url).header("Authorization", current.bearer()),
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(get("/api/v1/profiles").param("userId", Long.toString(current.id()))
                .header("Authorization", current.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/profiles").param("userId", Long.toString(other.id()))
                .header("Authorization", other.bearer())).andExpect(status().is4xxClientError());
        mvc.perform(json(post(AUTH + "/sign-in"), credentials(email, PASSWORD)))
                .andExpect(status().is4xxClientError());
        signIn(email, NEW_PASSWORD);
    }

    @Test
    void avatarUploadRejectsInvalidFilesAndPersistsTheImage() throws Exception {
        String email = "profile-avatar@example.com";
        register(email);
        var session = signIn(email, PASSWORD);
        String profilePath = "/api/v1/profiles";
        var profile = mvc.perform(get(profilePath).param("userId", Long.toString(session.id()))
                .header("Authorization", session.bearer())).andExpect(status().isOk()).andReturn();
        profilePath += "/" + json.readTree(profile.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(multipart(profilePath + "/avatar")
                .file(new MockMultipartFile("image", "bad.png", "image/png", new byte[] {1, 2, 3}))
                .header("Authorization", session.bearer())).andExpect(status().isBadRequest());

        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var uploaded = mvc.perform(multipart(profilePath + "/avatar")
                .file(new MockMultipartFile("image", "avatar.png", "image/png", bytes.toByteArray()))
                .header("Authorization", session.bearer())).andExpect(status().isOk()).andReturn();
        var url = json.readTree(uploaded.getResponse().getContentAsString()).get("avatarUrl").asText();
        mvc.perform(get(profilePath).header("Authorization", session.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").value(url));
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(bytes.toByteArray()));
    }

    private void register(String email) throws Exception {
        mvc.perform(json(post(AUTH + "/sign-up"), credentials(email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private Session signIn(String email, String password) throws Exception {
        var response = mvc.perform(json(post(AUTH + "/sign-in"), credentials(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse();
        var session = json.readTree(response.getContentAsString());
        return new Session(session.get("id").asLong(), "Bearer " + session.get("token").asText());
    }

    private void requestEmail(Session session, String email) throws Exception {
        mvc.perform(json(post(EMAIL_CHANGE).header("Authorization", session.bearer()),
                "{\"email\":\"" + email + "\"}")).andExpect(status().isAccepted());
    }

    private static String confirmationPath(Path inbox) throws Exception {
        return java.net.URI.create(Files.readAllLines(inbox).getLast()).getPath();
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType("application/json").content(body);
    }

    private record Session(long id, String bearer) {}
}
