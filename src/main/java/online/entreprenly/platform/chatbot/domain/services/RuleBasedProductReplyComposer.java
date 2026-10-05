package online.entreprenly.platform.chatbot.domain.services;

import online.entreprenly.platform.chatbot.domain.model.valueobjects.CatalogProduct;
import online.entreprenly.platform.chatbot.domain.model.valueobjects.OrderItem;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class RuleBasedProductReplyComposer implements ProductReplyComposer {

    private static final Pattern NUMBER = Pattern.compile("(?<![\\w])(\\d+(?:[.,]\\d+)?)(?![\\w])");


    private static final Map<String, Double> NUMBER_WORDS = Map.ofEntries(
            Map.entry("un", 1.0), Map.entry("una", 1.0), Map.entry("uno", 1.0),
            Map.entry("dos", 2.0), Map.entry("tres", 3.0), Map.entry("cuatro", 4.0),
            Map.entry("cinco", 5.0), Map.entry("seis", 6.0), Map.entry("siete", 7.0),
            Map.entry("ocho", 8.0), Map.entry("nueve", 9.0), Map.entry("diez", 10.0),
            Map.entry("once", 11.0), Map.entry("doce", 12.0), Map.entry("trece", 13.0),
            Map.entry("catorce", 14.0), Map.entry("quince", 15.0), Map.entry("dieciseis", 16.0),
            Map.entry("diecisiete", 17.0), Map.entry("dieciocho", 18.0), Map.entry("diecinueve", 19.0),
            Map.entry("veinte", 20.0), Map.entry("treinta", 30.0), Map.entry("par", 2.0),
            Map.entry("docena", 12.0), Map.entry("media", 0.5), Map.entry("medio", 0.5));


    private static final String[] ORDER_INTENT = {
            "quiero", "kiero", "qiero", "quisiera", "deseo", "dame", "deme", "necesito", "comprar",
            "pedir", "pedido", "llevar", "llevo", "ponme", "mandame", "enviame", "envieme", "traeme",
            "separame", "vendeme", "vender", "me das", "me da ", "me vendes", "anota", "agrega",
            "kilos", "kilo", "kg", "unidad", "unidades", "docena", "paquete", "botella", "lata"};

    /** Topics the generic responder handles; a product reply would only get in the way. */
    private static final String[] NON_PRODUCT_TOPICS = {
            "mi pedido", "mi orden", "estado de", "seguimiento", "ya pague", "ya te pague", "comprobante",
            "como pago", "formas de pago", "metodos de pago", "yapee", "yapeo", "plinee",
            "delivery", "horario", "a que hora", "reclamo", "queja", "devolucion"};


    private static final Set<String> EXTRACTION_STOP_WORDS = Set.of(
            "quiero", "kiero", "qiero", "quisiera", "deseo", "dame", "deme", "necesito", "comprar",
            "pedir", "pedido", "llevar", "llevo", "ponme", "mandame", "enviame", "envieme", "traeme",
            "separame", "vendeme", "vender", "vendes", "das", "anota", "anotame", "agrega", "agregame",
            "kilos", "kilo", "unidad", "unidades", "docena", "docenas", "paquete", "paquetes",
            "botella", "botellas", "lata", "latas",
            "un", "una", "uno", "unos", "unas", "dos", "tres", "cuatro", "cinco", "seis", "siete",
            "ocho", "nueve", "diez", "once", "doce", "media", "medio", "par",
            "de", "del", "la", "el", "los", "las", "por", "para", "con", "favor", "porfavor", "porfa",
            "me", "kg", "mas", "nomas", "solo", "tambien", "otro", "otra", "otros", "otras",
            "ese", "esa", "eso", "esos", "esas", "este", "esta", "esto", "estos", "estas", "mismo", "misma",
            "hola", "buenas", "buenos", "dias", "tardes", "noches", "gracias", "bueno", "entonces",
            "tienen", "tiene", "tienes", "hay", "venden", "precio", "cuanto", "cuesta", "vale");

    @Override
    public Optional<String> compose(String incomingContent, List<CatalogProduct> catalog) {
        if (incomingContent == null || incomingContent.isBlank()) {
            return Optional.empty();
        }
        var text = normalize(incomingContent);
        var safeCatalog = catalog == null ? List.<CatalogProduct>of() : catalog;

        var match = bestMatch(text, safeCatalog);
        if (match != null) {
            return Optional.of(replyForProduct(text, match));
        }
        if (mentionsAny(text, NON_PRODUCT_TOPICS)) {
            return Optional.empty();
        }
        if (mentionsAny(text, "catalogo", "productos", "que venden", "que vende", "que vendes",
                "que tienes", "que tienen", "q tienes", "k tienes", "que hay", "menu", "lista", "ofrecen",
                "carta", "opciones", "muestrame")) {
            return Optional.of(replyWithCatalogue(safeCatalog));
        }

        if (mentionsAny(text, ORDER_INTENT)) {
            return Optional.of(replyWhenProductNotFound(safeCatalog, extractRequestedProduct(text)));
        }

        if (isProductIntent(text)) {
            return Optional.of(replyWhenProductNotFound(safeCatalog, null));
        }
        return Optional.empty();
    }

    @Override
    public Optional<OrderItem> detectOrder(String incomingContent, List<CatalogProduct> catalog) {
        return detectOrder(incomingContent, catalog, null);
    }

    @Override
    public Optional<OrderItem> detectOrder(String incomingContent, List<CatalogProduct> catalog,
                                           CatalogProduct contextProduct) {
        if (incomingContent == null || incomingContent.isBlank() || catalog == null || catalog.isEmpty()) {
            return Optional.empty();
        }
        var text = normalize(incomingContent);



        var product = bestMatch(text, catalog);
        boolean usingContext = false;
        if (product == null) {
            // "quiero un sporade" after asking about Coca Cola names another product: it must not
            // be read as "one more Coca Cola". Only bare quantities ("dame dos") use the context.
            if (extractRequestedProduct(text) != null) {
                return Optional.empty();
            }
            product = contextProduct;
            usingContext = true;
        }
        if (product == null) {
            return Optional.empty();
        }



        var quantity = usingContext ? anyQuantity(text, product) : orderQuantity(text, product);
        if (quantity.isEmpty()) {
            return Optional.empty();
        }
        double qty = quantity.get();
        if (qty <= 0 || !product.isInStock() || qty > product.availableStock()) {
            return Optional.empty();
        }
        return Optional.of(new OrderItem(product.name(), (int) Math.round(qty), product.price()));
    }

    @Override
    public Optional<CatalogProduct> matchProduct(String incomingContent, List<CatalogProduct> catalog) {
        if (incomingContent == null || incomingContent.isBlank() || catalog == null || catalog.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(bestMatch(normalize(incomingContent), catalog));
    }


    private boolean isProductIntent(String text) {
        return mentionsAny(text, "tienen", "tiene", "tienes", "precio", "cuanto cuesta", "cuesta",
                "cuanto esta", "a cuanto", "cuanto sale", "vale", "comprar", "vendes", "venden", "producto",
                "stock", "disponible", "disponibilidad", "mercaderia", "consigo", "venta", "quedan");
    }


    private String replyWhenProductNotFound(List<CatalogProduct> catalog, String requestedProduct) {
        var inStock = catalog.stream().filter(CatalogProduct::isInStock).toList();
        if (inStock.isEmpty()) {
            if (requestedProduct != null && !requestedProduct.isBlank()) {
                return "Lo sentimos, no contamos con %s ni con otros productos disponibles en este momento. ¿Puedo ayudarte con algo más?"
                        .formatted(requestedProduct);
            }
            return "Por ahora no contamos con productos disponibles. ¡Pronto tendremos novedades!";
        }
        var items = inStock.stream()
                .map(p -> "%s (%s %s)".formatted(p.name(), price(p.price()), p.soldByWeight() ? "por kg" : "c/u"))
                .collect(Collectors.joining(", "));
        if (requestedProduct != null && !requestedProduct.isBlank()) {
            return "No contamos con %s, pero sí tenemos: %s. ¿Te interesa alguno?".formatted(requestedProduct, items);
        }
        return "No tenemos ese producto por ahora, pero sí contamos con: %s. ¿Te interesa alguno?".formatted(items);
    }


    private String extractRequestedProduct(String text) {
        var sb = new StringBuilder();
        for (var word : text.split("[^a-zñ]+")) {
            if (word.length() >= 3 && !EXTRACTION_STOP_WORDS.contains(word)) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(word);
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String replyForProduct(String text, CatalogProduct product) {
        var priceLabel = product.soldByWeight() ? "por kg" : "c/u";
        var unitLabel = product.soldByWeight() ? "kg" : "unidades";

        var quantity = orderQuantity(text, product);
        if (quantity.isPresent()) {
            double qty = quantity.get();
            if (!product.isInStock() || qty > product.availableStock()) {
                return "Por ahora solo tenemos %s de %s. ¿Deseas ajustar la cantidad?"
                        .formatted(stock(product), product.name());
            }
            double total = Math.round(qty * product.price() * 100.0) / 100.0;
            return "Perfecto. %s de %s = %s. ¿Confirmas el pedido? ¿A qué dirección lo enviamos?"
                    .formatted(quantityLabel(qty, unitLabel), product.name(), price(total));
        }

        if (!product.isInStock()) {
            return "%s cuesta %s %s, pero por ahora no tenemos stock disponible."
                    .formatted(product.name(), price(product.price()), priceLabel);
        }
        return "Sí, tenemos %s a %s %s. Quedan %s disponibles. ¿Cuántos deseas?"
                .formatted(product.name(), price(product.price()), priceLabel, stock(product));
    }

    private String replyWithCatalogue(List<CatalogProduct> catalog) {
        var items = catalog.stream()
                .filter(CatalogProduct::isInStock)
                .map(p -> "%s (%s %s)".formatted(p.name(), price(p.price()), p.soldByWeight() ? "por kg" : "c/u"))
                .collect(Collectors.joining(", "));
        if (items.isBlank()) {
            return "Por ahora no tenemos productos con stock disponible.";
        }
        return "Tenemos disponible: %s. ¿Qué te gustaría pedir?".formatted(items);
    }

    /**
     * Picks the product whose name best matches the message, word by word. Plurals ("oreos"),
     * names written together ("cocacola") and small typos ("sprit") still count, while short
     * fragments inside unrelated words no longer do.
     */
    private CatalogProduct bestMatch(String text, List<CatalogProduct> catalog) {
        var textWords = words(text);
        CatalogProduct best = null;
        int bestScore = 0;
        for (var product : catalog) {
            var tokens = productTokens(product);
            var joinedName = String.join("", tokens);
            int score = 0;
            for (var token : tokens) {
                score += tokenScore(token, textWords, joinedName);
            }
            if (score > bestScore) {
                bestScore = score;
                best = product;
            }
        }
        return bestScore > 0 ? best : null;
    }

    /** 2 for an exact word (or its singular/plural), 1 for a prefix or a typo, 0 otherwise. */
    private static int tokenScore(String token, List<String> textWords, String joinedName) {
        int score = 0;
        for (var word : textWords) {
            if (word.length() < 3) continue;
            if (sameWord(token, word) || writtenTogether(token, word, joinedName)) {
                return 2;
            }
            if (prefixMatch(token, word) || typoMatch(token, word)) {
                score = 1;
            }
        }
        return score;
    }

    /** "cocacola" / "cocacolas" for "Coca Cola": the word is part of the name written without spaces. */
    private static boolean writtenTogether(String token, String word, String joinedName) {
        for (var form : variants(word)) {
            if (form.length() >= 6 && form.contains(token) && joinedName.contains(form)) return true;
        }
        return false;
    }

    private static boolean sameWord(String a, String b) {
        for (var x : variants(a)) {
            for (var y : variants(b)) {
                if (x.equals(y)) return true;
            }
        }
        return false;
    }

    /** The word plus its likely singular forms: "galletas" → "galleta", "limones" → "limon". */
    private static List<String> variants(String word) {
        var forms = new ArrayList<String>();
        forms.add(word);
        if (word.length() > 3 && word.endsWith("s")) forms.add(word.substring(0, word.length() - 1));
        if (word.length() > 4 && word.endsWith("es")) forms.add(word.substring(0, word.length() - 2));
        return forms;
    }

    /** "coca" ↔ "cocas", "gallet" ↔ "galletas": a shared start of at least 4 letters, short tail. */
    private static boolean prefixMatch(String token, String word) {
        var shorter = token.length() <= word.length() ? token : word;
        var longer = shorter == token ? word : token;
        return shorter.length() >= 4 && longer.startsWith(shorter) && longer.length() - shorter.length() <= 3;
    }

    /** One wrong letter in words of 5+ letters, two in words of 8+. */
    private static boolean typoMatch(String token, String word) {
        int longest = Math.max(token.length(), word.length());
        int allowed = longest >= 8 ? 2 : longest >= 5 ? 1 : 0;
        return allowed > 0 && Math.abs(token.length() - word.length()) <= allowed
                && editDistance(token, word) <= allowed;
    }

    private static int editDistance(String a, String b) {
        var previous = new int[b.length() + 1];
        var current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            var swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    private static List<String> productTokens(CatalogProduct product) {
        return words(normalize(product.name())).stream().filter(token -> token.length() >= 3).toList();
    }

    private static List<String> words(String text) {
        return List.of(text.split("[^a-z0-9ñ]+"));
    }


    private Optional<Double> orderQuantity(String text, CatalogProduct product) {
        if (!mentionsAny(text, ORDER_INTENT)) {
            return Optional.empty();
        }
        return anyQuantity(text, product);
    }


    private Optional<Double> anyQuantity(String text, CatalogProduct product) {

        var cleaned = text;
        for (var token : productTokens(product)) {
            cleaned = cleaned.replace(token, " ");
        }
        Matcher matcher = NUMBER.matcher(cleaned);
        if (matcher.find()) {
            return Optional.of(Double.parseDouble(matcher.group(1).replace(',', '.')));
        }
        if (cleaned.contains("media docena")) {
            return Optional.of(6.0);
        }

        for (var word : cleaned.split("[^a-zñ]+")) {
            var value = NUMBER_WORDS.get(word);
            if (value != null) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private String stock(CatalogProduct product) {
        return product.soldByWeight()
                ? String.format(Locale.US, "%.1f kg", product.availableStock())
                : String.format(Locale.US, "%d unidades", (long) product.availableStock());
    }

    private String quantityLabel(double quantity, String unitLabel) {
        return unitLabel.equals("kg")
                ? String.format(Locale.US, "%.1f kg", quantity)
                : String.format(Locale.US, "%d unidades", (long) quantity);
    }


    private static String price(double value) {
        return String.format(Locale.US, "S/%.2f", value);
    }

    private static boolean mentionsAny(String text, String... keywords) {
        for (var keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }


    private static String normalize(String value) {
        var lower = value.toLowerCase(Locale.ROOT);
        var decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
