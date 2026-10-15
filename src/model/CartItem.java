package model;

public final class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        setQuantity(quantity);
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        if(quantity<1)throw new IllegalArgumentException("Quantity must be at least 1.");
        this.quantity = quantity;
    }
    public double getSubtotal() { return product.getPrice() * quantity; }
}
