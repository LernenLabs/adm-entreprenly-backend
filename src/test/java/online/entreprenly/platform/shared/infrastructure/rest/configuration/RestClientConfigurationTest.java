package online.entreprenly.platform.shared.infrastructure.rest.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RestClientConfigurationTest {

    @Test
    void restClientBuilderReturnsNonNullInstance() {
        var config = new RestClientConfiguration();
        var builder = config.restClientBuilder();
        assertNotNull(builder);
        assertNotNull(builder.build());
    }
}
