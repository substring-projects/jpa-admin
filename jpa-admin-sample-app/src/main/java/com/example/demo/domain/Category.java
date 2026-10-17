package com.example.demo.domain;

import io.github.jpaadmin.annotations.AdminField;
import io.github.jpaadmin.annotations.AdminResource;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "categories")
@AdminResource(
    displayName = "Categories",
    pluralName = "Categories",
    group = "Catalog",
    icon = "folder"
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @AdminField(label = "ID", order = 1, sortable = true)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank
    @AdminField(label = "Category Name", order = 2, searchable = true, sortable = true)
    private String name;

    @Column(length = 500)
    @AdminField(label = "Description", order = 3)
    private String description;

    public Category() {}

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return name != null ? name : ("Category #" + id);
    }
}
