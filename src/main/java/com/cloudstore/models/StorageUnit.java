package com.cloudstore.models;

/**
 * Represents a self-storage unit.
 */
public class StorageUnit {
    private String id;
    private String name;
    private String size; // e.g., Small, Medium, Large
    private double price;
    private boolean available;

    // Default constructor for Jackson
    public StorageUnit() {}

    public StorageUnit(String id, String name, String size, double price, boolean available) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.price = price;
        this.available = available;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
