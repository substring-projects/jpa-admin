package com.substringtechnologies.jpaadmin.sample.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.substringtechnologies.jpaadmin.annotations.ActionScope;
import com.substringtechnologies.jpaadmin.annotations.AdminAction;
import com.substringtechnologies.jpaadmin.annotations.AdminBadge;
import com.substringtechnologies.jpaadmin.annotations.AdminComputed;
import com.substringtechnologies.jpaadmin.annotations.AdminField;
import com.substringtechnologies.jpaadmin.annotations.AdminLookup;
import com.substringtechnologies.jpaadmin.annotations.AdminUpload;
import com.substringtechnologies.jpaadmin.annotations.AdminResource;
import com.substringtechnologies.jpaadmin.annotations.AdminWidget;
import com.substringtechnologies.jpaadmin.annotations.BadgeColor;
import com.substringtechnologies.jpaadmin.annotations.BadgeMap;
import com.substringtechnologies.jpaadmin.annotations.SortDir;
import com.substringtechnologies.jpaadmin.annotations.WidgetType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

    @Column(length = 500)
    @AdminField(label = "Image", order = 2, description = "Shown on the storefront and in search results")
    @AdminUpload(folder = "products", maxSize = "5MB")
    private String imageUrl;

    @Column(nullable = false)
    @NotBlank
    @AdminField(label = "Product Title", order = 3, searchable = true, sortable = true)
    private String name;

    @Lob
    @Column(length = 2000)
    @AdminField(label = "Description", order = 4, inList = false)
    @AdminWidget(type = WidgetType.TEXTAREA)
    private String description;

    @NotNull
    @Positive
    @AdminField(label = "Price ($)", order = 5, sortable = true, filterable = true)
    private BigDecimal price;

    @NotNull
    @Min(0)
    @AdminField(label = "Stock Qty", order = 6, sortable = true)
    @AdminWidget(min = 0, step = 1)
    private Integer stockQuantity;

    @AdminField(label = "Discount (%)", order = 7, inList = false, description = "Applied at checkout")
    @AdminWidget(type = WidgetType.SLIDER, min = 0, max = 90, step = 5)
    private Integer discountPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @AdminField(label = "Status", order = 7, filterable = true)
    @AdminBadge(mapping = {
            @BadgeMap(value = "DRAFT", color = BadgeColor.GRAY, label = "Draft"),
            @BadgeMap(value = "PUBLISHED", color = BadgeColor.GREEN, label = "Published"),
            @BadgeMap(value = "OUT_OF_STOCK", color = BadgeColor.RED, label = "Out of Stock"),
            @BadgeMap(value = "ARCHIVED", color = BadgeColor.YELLOW, label = "Archived")
    })
    private ProductStatus status = ProductStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @AdminField(label = "Category", order = 8, filterable = true)
    @AdminLookup(lookupProperty = "name", searchProperty = "name")
    private Category category;

    @Column(length = 4000)
    @AdminField(label = "Marketing Copy", order = 12, inList = false, description = "Formatted text shown on the storefront")
    @AdminWidget(type = WidgetType.RICH_TEXT)
    private String marketingCopy;

    @Column(length = 4000)
    @AdminField(label = "Specifications (JSON)", order = 13, inList = false)
    @AdminWidget(type = WidgetType.CODE_EDITOR, language = "json")
    private String specifications;

    @Column(length = 500)
    @AdminField(label = "Datasheet", order = 14, inList = false, description = "Technical specifications customers can download")
    @AdminUpload(folder = "datasheets", accept = "application/pdf", maxSize = "10MB")
    private String datasheetUrl;

    @ManyToMany
    @JoinTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @AdminField(label = "Tags", order = 11)
    @AdminLookup(lookupProperty = "name")
    private Set<Tag> tags = new LinkedHashSet<>();

    @Column(updatable = false)
    @AdminField(label = "Created At", order = 9, readOnly = true, inForm = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(insertable = false)
    @AdminField(label = "Created At (Instant)", order = 10, readOnly = true, inForm = false)
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
    @AdminComputed(label = "In Stock?", order = 10)
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    @Transient
    @AdminComputed(label = "Inventory Value", order = 11, inList = false, format = "$%,.2f")
    public BigDecimal getInventoryValue() {
        return price == null || stockQuantity == null ? null : price.multiply(BigDecimal.valueOf(stockQuantity));
    }

    @AdminAction(name = "Publish Product", icon = "circle-check", scope = ActionScope.BOTH)
    public void publish() {
        // Shown to the admin as the action's error message.
        if (status == ProductStatus.ARCHIVED) throw new IllegalStateException("Archived products can't be published");
        this.status = ProductStatus.PUBLISHED;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
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

    public String getMarketingCopy() { return marketingCopy; }
    public void setMarketingCopy(String marketingCopy) { this.marketingCopy = marketingCopy; }
    public String getSpecifications() { return specifications; }
    public void setSpecifications(String specifications) { this.specifications = specifications; }

    public String getDatasheetUrl() { return datasheetUrl; }
    public void setDatasheetUrl(String datasheetUrl) { this.datasheetUrl = datasheetUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }

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
