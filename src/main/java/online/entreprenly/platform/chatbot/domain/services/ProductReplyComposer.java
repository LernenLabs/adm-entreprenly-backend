package online.entreprenly.platform.chatbot.domain.services;

import online.entreprenly.platform.chatbot.domain.model.valueobjects.CatalogProduct;
import online.entreprenly.platform.chatbot.domain.model.valueobjects.OrderItem;

import java.util.List;
import java.util.Optional;


public interface ProductReplyComposer {

    
    Optional<String> compose(String incomingContent, List<CatalogProduct> catalog);

    
    Optional<OrderItem> detectOrder(String incomingContent, List<CatalogProduct> catalog);

    
    Optional<OrderItem> detectOrder(String incomingContent, List<CatalogProduct> catalog, CatalogProduct contextProduct);

    /** Every product a message orders, one per line ("- 1 unidad de Agua", "- 2 unidades de Atún"). */
    default List<OrderItem> detectOrders(String incomingContent, List<CatalogProduct> catalog) {
        return detectOrder(incomingContent, catalog).map(List::of).orElseGet(List::of);
    }

    
    Optional<CatalogProduct> matchProduct(String incomingContent, List<CatalogProduct> catalog);

    /** The products in stock, listed one per line, ready to send. */
    String catalogue(List<CatalogProduct> catalog);

    /** Answer for a message nothing else understood, so the conversation never dead-ends. */
    Optional<String> replyToUnknown(String incomingContent, List<CatalogProduct> catalog);
}
