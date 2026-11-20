package com.shoplite.catalog;

/** A group of products, such as "Kitchen". Kept in memory for now. */
public class Category {

    private Long id;
    private String name;
    private String description;

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    // Only the repository sets the id, when it stores a new category.
    void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
