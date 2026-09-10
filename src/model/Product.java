package model;

public final class Product {
    private final int id;
    private String name;
    private String category;
    private String subcategory;
    private double price;
    private int stock;
    private String imagePath;

    public Product(int id, String name, String category, String subcategory,
                   double price, int stock, String imagePath) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.subcategory = subcategory;
        this.price = price;
        this.stock = stock;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getSubcategory() { return subcategory; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getImagePath() { return imagePath; }
    public boolean isAvailable() { return stock > 0; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    public void setPrice(double price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
