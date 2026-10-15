package com.example.demo.domain;

import io.github.jpaadmin.annotations.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@AdminResource(
    displayName = "Order",
    pluralName = "Orders",
    group = "Sales",
    icon = "shopping-cart",
    defaultSortBy = "id",
    defaultSortDir = SortDir.DESC
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @AdminField(label = "ID", order = 1, sortable = true)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank
    @AdminField(label = "Order #", order = 2, searchable = true, sortable = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull
    @AdminField(label = "Customer", order = 3, filterable = true)
    @AdminLookup(lookupProperty = "fullName", searchProperty = "fullName", quickCreate = true)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @AdminField(label = "Order Status", order = 4, filterable = true)
    @AdminBadge(mapping = {
        @BadgeMap(value = "PENDING", color = BadgeColor.YELLOW, label = "Pending"),
        @BadgeMap(value = "PAID", color = BadgeColor.BLUE, label = "Paid"),
        @BadgeMap(value = "SHIPPED", color = BadgeColor.INDIGO, label = "Shipped"),
        @BadgeMap(value = "DELIVERED", color = BadgeColor.GREEN, label = "Delivered"),
        @BadgeMap(value = "CANCELLED", color = BadgeColor.RED, label = "Cancelled")
    })
    private OrderStatus status = OrderStatus.PENDING;

    @NotNull
    @AdminField(label = "Total ($)", order = 5, sortable = true)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(updatable = false)
    @AdminField(label = "Order Date", order = 6, readOnly = true, inForm = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @AdminInline(
        type = InlineType.TABULAR,
        title = "Order Line Items",
        canAdd = true,
        canDelete = true,
        limit = 50
    )
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Order(String orderNumber, Customer customer, OrderStatus status, BigDecimal totalAmount) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
