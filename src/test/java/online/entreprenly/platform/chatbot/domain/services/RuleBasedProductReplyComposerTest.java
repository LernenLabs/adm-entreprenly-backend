package online.entreprenly.platform.chatbot.domain.services;

import online.entreprenly.platform.chatbot.domain.model.valueobjects.CatalogProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedProductReplyComposerTest {

    private final ProductReplyComposer composer = new RuleBasedProductReplyComposer();

    private static final List<CatalogProduct> CATALOG = List.of(
            new CatalogProduct("Manzana", 4.50, true, 20.0),
            new CatalogProduct("Pan", 0.50, false, 30.0),
            new CatalogProduct("Coca Cola 500ml", 2.50, false, 0.0));

    @Test
    @DisplayName("answers price and stock for a product question")
    void answersPriceAndStock() {
        var reply = composer.compose("¿Tienen manzana? ¿a cuánto?", CATALOG);
        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("4.50").contains("20.0 kg");
    }

    @Test
    @DisplayName("computes the total for an order quantity")
    void computesOrderTotal() {
        var reply = composer.compose("Hola, quiero 5 kilos de manzana", CATALOG);
        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("22.50");
    }

    @Test
    @DisplayName("warns when the requested quantity exceeds stock")
    void warnsWhenOverStock() {
        var reply = composer.compose("quiero 50 kilos de manzana", CATALOG);
        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("20.0 kg").containsIgnoringCase("ajustar");
    }

    @Test
    @DisplayName("lists the available catalogue, hiding out-of-stock products")
    void listsCatalogue() {
        var reply = composer.compose("¿qué venden?", CATALOG);
        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("Manzana").contains("Pan").doesNotContain("Coca Cola");
    }

    @Test
    @DisplayName("reports when a known product is out of stock")
    void reportsOutOfStock() {
        var reply = composer.compose("tienen coca cola?", CATALOG);
        assertThat(reply).isPresent();
        assertThat(reply.get()).containsIgnoringCase("no tenemos stock");
    }

    @Test
    @DisplayName("returns empty for non-product messages so the generic reply takes over")
    void fallsBackForNonProduct() {
        assertThat(composer.compose("buenas tardes", CATALOG)).isEmpty();
        assertThat(composer.compose("hola, gracias", List.of())).isEmpty();
    }

    @Test
    @DisplayName("suggests the available catalogue when the asked product is not found")
    void suggestsWhenProductNotFound() {
        var reply = composer.compose("tienen pepsi?", CATALOG);
        assertThat(reply).isPresent();
        assertThat(reply.get()).containsIgnoringCase("no tenemos ese producto").contains("Manzana");
    }

    @Test
    @DisplayName("informs when the seller has no products at all")
    void informsWhenNoProducts() {
        var reply = composer.compose("tienen pepsi?", List.of());
        assertThat(reply).isPresent();
        assertThat(reply.get()).containsIgnoringCase("no contamos con productos");
    }

    private static final List<CatalogProduct> STORE = List.of(
            new CatalogProduct("Galletas Oreo", 2.50, false, 20.0),
            new CatalogProduct("Coca Cola 500ml", 3.50, false, 10.0),
            new CatalogProduct("Platano de Seda", 3.00, true, 15.0),
            new CatalogProduct("Sprite 500ml", 3.00, false, 8.0),
            new CatalogProduct("Pan", 0.50, false, 30.0));

    @Test
    @DisplayName("understands plurals, names written together and small typos")
    void understandsHowClientsActuallyWrite() {
        var oreos = composer.detectOrder("quiero 2 oreos", STORE).orElseThrow();
        assertThat(oreos.productName()).isEqualTo("Galletas Oreo");
        assertThat(oreos.quantity()).isEqualTo(2);

        var cocas = composer.detectOrder("dame 3 cocacolas porfa", STORE).orElseThrow();
        assertThat(cocas.productName()).isEqualTo("Coca Cola 500ml");
        assertThat(cocas.quantity()).isEqualTo(3);

        assertThat(composer.detectOrder("quiero 2 kilos de platanos", STORE).orElseThrow().productName())
                .isEqualTo("Platano de Seda");
        assertThat(composer.matchProduct("tienen sprit?", STORE).orElseThrow().name()).isEqualTo("Sprite 500ml");
        assertThat(composer.detectOrder("media docena de galletas oreo", STORE).orElseThrow().quantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("does not turn a product it does not sell into the previously discussed one")
    void unknownProductIsNotReadAsTheContextProduct() {
        var coca = STORE.get(1);
        assertThat(composer.detectOrder("quiero un sporade", STORE, coca)).isEmpty();
        assertThat(composer.compose("quiero un sporade", STORE).orElseThrow())
                .contains("No contamos con sporade").contains("Galletas Oreo");

        // A bare quantity still refers to the product being discussed.
        var more = composer.detectOrder("dame dos", STORE, coca).orElseThrow();
        assertThat(more.productName()).isEqualTo("Coca Cola 500ml");
        assertThat(more.quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("leaves order status, payment and unrelated chat to the generic responder")
    void ignoresNonProductMessages() {
        assertThat(composer.compose("donde esta mi pedido", STORE)).isEmpty();
        assertThat(composer.compose("ya te yapee", STORE)).isEmpty();
        assertThat(composer.compose("me voy a la playa", STORE)).isEmpty();
        assertThat(composer.compose("q tienes?", STORE).orElseThrow()).startsWith("Tenemos disponible");
    }

    private static final List<CatalogProduct> SHOP = List.of(
            new CatalogProduct("Coca-Cola 500ml", 3.50, false, 10.0),
            new CatalogProduct("Leche Gloria Entera", 4.20, false, 30.0),
            new CatalogProduct("Leche Gloria 2L", 6.00, false, 12.0),
            new CatalogProduct("Manzana Roja", 4.50, true, 20.0),
            new CatalogProduct("Galletas Oreo", 2.50, false, 20.0));

    @Test
    @DisplayName("a quantity first is an order, and the size picks the right product")
    void ordersWithoutOrderWords() {
        var liters = composer.detectOrder("3 Leches Glorias 2L", SHOP).orElseThrow();
        assertThat(liters.productName()).isEqualTo("Leche Gloria 2L");
        assertThat(liters.quantity()).isEqualTo(3);

        assertThat(composer.detectOrder("me gustaria 2 oreos", SHOP).orElseThrow().productName())
                .isEqualTo("Galletas Oreo");
    }

    @Test
    @DisplayName("a size is not the quantity")
    void sizeIsNotQuantity() {
        var coca = composer.detectOrder("Quiero una gaseosa Coca Cola de 500 ml", SHOP).orElseThrow();
        assertThat(coca.productName()).isEqualTo("Coca-Cola 500ml");
        assertThat(coca.quantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("asks which one instead of guessing between similar products")
    void asksWhenAmbiguous() {
        assertThat(composer.detectOrder("3 leches gloria", SHOP)).isEmpty();
        assertThat(composer.compose("3 leches gloria", SHOP).orElseThrow())
                .contains("varias opciones").contains("Leche Gloria Entera").contains("Leche Gloria 2L");
    }

    @Test
    @DisplayName("answers a generic category with its products")
    void answersCategories() {
        assertThat(composer.compose("1 gaseosa", SHOP).orElseThrow())
                .contains("Coca-Cola 500ml").doesNotContain("Leche");
        assertThat(composer.compose("que frutas tienes", SHOP).orElseThrow()).contains("Manzana Roja");
    }

    @Test
    @DisplayName("names the unknown product, and never dead-ends on what it cannot understand")
    void repliesToUnknownMessages() {
        assertThat(composer.replyToUnknown("gatorade", SHOP).orElseThrow())
                .contains("No contamos con gatorade").contains("• Galletas Oreo");
        assertThat(composer.replyToUnknown("Un six pack de Sporade's", SHOP).orElseThrow())
                .contains("No contamos con sporade");
        assertThat(composer.replyToUnknown("el perro de mi vecino ladra mucho toda la noche", SHOP).orElseThrow())
                .contains("No logré entender").contains("• Galletas Oreo");
    }
}
