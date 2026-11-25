package online.entreprenly.platform.inventory.domain.model.commands;

/**
 * Command to create a new weight product for an owner account.
 *
 * @param ownerEmail  the email of the account that owns the product
 * @param name        the product display name
 * @param description the product description
 * @param codeQR      the QR code identifying the product
 * @param pricePerKg  the price per kilogram
 * @param imageUrl    the public URL of the product photo
 */
public record CreateWeightProductCommand(String ownerEmail, String name, String description, String codeQR,
                                         double pricePerKg, String imageUrl) {
}
