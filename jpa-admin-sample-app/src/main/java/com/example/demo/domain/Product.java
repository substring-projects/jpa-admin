package com.example.demo.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import io.github.jpaadmin.annotations.ActionScope;
import io.github.jpaadmin.annotations.AdminAction;
import io.github.jpaadmin.annotations.AdminBadge;
import io.github.jpaadmin.annotations.AdminComputed;
import io.github.jpaadmin.annotations.AdminField;
import io.github.jpaadmin.annotations.AdminLookup;
import io.github.jpaadmin.annotations.AdminResource;
import io.github.jpaadmin.annotations.AdminWidget;
import io.github.jpaadmin.annotations.BadgeColor;
import io.github.jpaadmin.annotations.BadgeMap;
import io.github.jpaadmin.annotations.SortDir;
import io.github.jpaadmin.annotations.WidgetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "products")
@AdminResource(displayName = "Product", pluralName = "Products", group = "Catalog", icon = "package", defaultSortBy = "id", defaultSortDir = SortDir.DESC)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @AdminField(label = "ID", order = 1, sortable = true)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    @AdminField(label = "Product Title", order = 2, searchable = true, sortable = true)
    private String name;

    @Lob
    @Column(length = 2000)
    @AdminField(label = "Description", order = 3, inList = false)
    @AdminWidget(type = WidgetType.TEXTAREA)
    private String description;

    @NotNull
    @Positive
    @AdminField(label = "Price ($)", order = 4, sortable = true, filterable = true)
    private BigDecimal price;

    @NotNull
    @Min(0)
    @AdminField(label = "Stock Qty", order = 5, sortable = true)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @AdminField(label = "Status", order = 6, filterable = true)
    @AdminBadge(mapping = {
            @BadgeMap(value = "DRAFT", color = BadgeColor.GRAY, label = "Draft"),
            @BadgeMap(value = "PUBLISHED", color = BadgeColor.GREEN, label = "Published"),
            @BadgeMap(value = "OUT_OF_STOCK", color = BadgeColor.RED, label = "Out of Stock"),
            @BadgeMap(value = "ARCHIVED", color = BadgeColor.YELLOW, label = "Archived")
    })
    private ProductStatus status = ProductStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @AdminField(label = "Category", order = 7, filterable = true)
    @AdminLookup(lookupProperty = "name", searchProperty = "name", quickCreate = true)
    private Category category;

    @Column(updatable = false)
    @AdminField(label = "Created At", order = 8, readOnly = true, inForm = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(insertable = false)
    @AdminField(label = "Created At (Instant)", order = 9, readOnly = true, inForm = false)
    private Instant createdAtInstant = Instant.now();

    public Product() {
    }

    public Product(String name, String description, BigDecimal price, Integer stockQuantity, ProductStatus status,
            Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.status = status;
        this.category = category;
        this.createdAt = LocalDateTime.now();
        this.createdAtInstant = Instant.now();
    }

    @Transient
    @AdminComputed(label = "In Stock?", order = 9)
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    @AdminAction(name = "Publish Product", icon = "check-circle", scope = ActionScope.BOTH)
    public void publish() {
        this.status = ProductStatus.PUBLISHED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCreatedAtInstant() {
        return createdAtInstant;
    }

    public void setCreatedAtInstant(Instant createdAtInstant) {
        this.createdAtInstant = createdAtInstant;
    }
}
