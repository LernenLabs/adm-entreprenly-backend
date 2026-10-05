package online.entreprenly.platform.chatbot.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedChatbotResponderTest {

    private final ChatbotResponder responder = new RuleBasedChatbotResponder();

    @Test
    @DisplayName("greets the client by name")
    void greetsClient() {
        var reply = responder.reply("Hola, buenas tardes", "Andrea");
        assertThat(reply).contains("Andrea").containsIgnoringCase("bienvenido");
    }

    @Test
    @DisplayName("acknowledges an order intent")
    void acknowledgesOrder() {
        var reply = responder.reply("Quiero comprar 3 gaseosas", null);
        assertThat(reply).containsIgnoringCase("pedido");
    }

    @Test
    @DisplayName("falls back to a generic reply for unknown intents")
    void fallsBack() {
        var reply = responder.reply("asdfghjkl", null);
        assertThat(reply).isNotBlank();
    }

    @Test
    @DisplayName("matches whole words, not fragments inside other words")
    void matchesWholeWordsOnly() {
        // "hay" used to fire inside "playa" and answer about stock.
        assertThat(responder.reply("me voy a la playa", null)).doesNotContain("disponibilidad");
    }

    @Test
    @DisplayName("understands common informal greetings and payment phrases")
    void understandsInformalPhrases() {
        assertThat(responder.reply("ola", "Ana")).contains("Ana").containsIgnoringCase("bienvenido");
        assertThat(responder.reply("ya te yapee", null)).containsIgnoringCase("comprobante");
        assertThat(responder.reply("a que numero te yapeo", null)).containsIgnoringCase("yape");
    }
}
