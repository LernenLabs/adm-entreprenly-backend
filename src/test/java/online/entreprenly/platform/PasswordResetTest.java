package online.entreprenly.platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.profiles.active=av2",
    "spring.datasource.url=jdbc:h2:mem:password_reset;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "profile.verification.inbox=target/password-reset-test-inbox"
})
@AutoConfigureMockMvc
class PasswordResetTest {
    private static final String AUTH = "/api/v1/authentication";
    private static final String RESET = AUTH + "/password-resets";
    private static final String EMAIL = "password-reset@example.com";
    private static final String ORIGINAL = "OriginalPass123";
    private static final String REPLACEMENT = "ReplacementPass456";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate database;
    @Autowired ObjectMapper json;

    @Test
    void requestIsGenericAndOnlyAFreshLinkCanResetPasswordAndRevokeSessions() throws Exception {
        mvc.perform(json(post(AUTH + "/sign-up"), """
                {"email":"%s","password":"%s","firstName":"Demo","lastName":"Reset"}
                """.formatted(EMAIL, ORIGINAL))).andExpect(status().isCreated());
        var session = json.readTree(mvc.perform(json(post(AUTH + "/sign-in"), credentials(ORIGINAL)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long id = session.get("id").asLong();
        String bearer = "Bearer " + session.get("token").asText();
        String requested = request(EMAIL);
        assertEquals(requested, request("unknown@example.com"));
        Path message = Path.of("target/password-reset-test-inbox/password-reset-" + id + ".txt");
        try {
            String expired = tokenFrom(message);
            database.update("update verification_challenges set issued_at = ? where id = ?",
                    java.sql.Timestamp.from(Instant.now().minusSeconds(700)), "password-reset:" + id);
            mvc.perform(json(post(RESET + "/validate"), link(id, expired))).andExpect(status().isGone());

            request(EMAIL);
            String token = tokenFrom(message);
            mvc.perform(json(post(RESET + "/validate"), link(id, token))).andExpect(status().isNoContent());
            mvc.perform(json(post(RESET + "/confirm"), """
                    {"userId":%d,"token":"%s","newPassword":"%s"}
                    """.formatted(id, token, REPLACEMENT))).andExpect(status().isNoContent());

            mvc.perform(json(post(RESET + "/validate"), link(id, token))).andExpect(status().isGone());
            mvc.perform(get("/api/v1/profiles?userId=" + id).header("Authorization", bearer))
                    .andExpect(status().isUnauthorized());
            mvc.perform(json(post(AUTH + "/sign-in"), credentials(ORIGINAL))).andExpect(status().isBadRequest());
            mvc.perform(json(post(AUTH + "/sign-in"), credentials(REPLACEMENT))).andExpect(status().isOk());

            request(EMAIL);
            String browserLink = RESET + "/open/" + id + "/" + tokenFrom(message);
            mvc.perform(get(browserLink)).andExpect(status().isOk());
            mvc.perform(post(browserLink).contentType("application/x-www-form-urlencoded")
                    .param("newPassword", ORIGINAL).param("confirmation", REPLACEMENT)).andExpect(status().isBadRequest());
            mvc.perform(get(browserLink)).andExpect(status().isOk());
            mvc.perform(post(browserLink).contentType("application/x-www-form-urlencoded")
                    .param("newPassword", ORIGINAL).param("confirmation", ORIGINAL)).andExpect(status().isOk());
            mvc.perform(get(browserLink)).andExpect(status().isGone());
            mvc.perform(json(post(AUTH + "/sign-in"), credentials(ORIGINAL))).andExpect(status().isOk());
        } finally {
            Files.deleteIfExists(message);
        }
    }

    private String request(String email) throws Exception {
        return mvc.perform(json(post(RESET), """
                {"email":"%s"}
                """.formatted(email))).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
    }

    private static String tokenFrom(Path message) throws Exception {
        String link = Files.readAllLines(message).getLast();
        return link.substring(link.lastIndexOf('/') + 1);
    }

    private static String credentials(String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, password);
    }

    private static String link(long id, String token) {
        return """
                {"userId":%d,"token":"%s"}
                """.formatted(id, token);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType("application/json").content(body);
    }
}
