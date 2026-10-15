package com.example.demo.config;

import com.example.demo.domain.*;
import jakarta.persistence.EntityManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EntityManager entityManager;

    public DataInitializer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Categories
        Category electronics = new Category("Electronics", "Phones, laptops, gadgets and smart accessories");
        Category fashion = new Category("Fashion & Apparel", "Men and women clothing, footwear, and accessories");
        Category home = new Category("Home & Kitchen", "Appliances, furniture, cookware and dining");
        entityManager.persist(electronics);
        entityManager.persist(fashion);
        entityManager.persist(home);

        // 2. Products
        Product p1 = new Product(
                "iPhone 16 Pro Max",
                "Titanium frame, A18 Pro chip, 48MP Fusion Camera, and Super Retina XDR OLED display.",
                new BigDecimal("1199.00"),
                45,
                ProductStatus.PUBLISHED,
                electronics
        );

        Product p2 = new Product(
                "MacBook Pro 16\" M3 Max",
                "Apple M3 Max chip with 16-core CPU, 40-core GPU, 48GB unified memory, and 1TB SSD.",
                new BigDecimal("3499.00"),
                12,
                ProductStatus.PUBLISHED,
                electronics
        );

        Product p3 = new Product(
                "Sony WH-1000XM5 Noise Canceling",
                "Industry leading noise canceling headphones with dual processors and 8 microphones.",
                new BigDecimal("399.99"),
                80,
                ProductStatus.PUBLISHED,
                electronics
        );

        Product p4 = new Product(
                "Italian Wool Overcoat",
                "Tailored 100% merino wool overcoat with horn buttons and satin lining.",
                new BigDecimal("495.00"),
                0,
                ProductStatus.OUT_OF_STOCK,
                fashion
        );

        Product p5 = new Product(
                "Smart Espresso Machine",
                "Direct drive dual boiler espresso machine with PID temperature control and touch display.",
                new BigDecimal("1250.00"),
                18,
                ProductStatus.DRAFT,
                home
        );

        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.persist(p3);
        entityManager.persist(p4);
        entityManager.persist(p5);

        // 3. Customers
        Customer c1 = new Customer("Durgesh Kumar", "durgesh@example.com", "+1-555-0199", "secretPass123", true);
        Customer c2 = new Customer("Sarah Connor", "sarah@cyberdyne.org", "+1-555-0142", "passSarah!45", true);
        Customer c3 = new Customer("Alex Mercer", "alex.mercer@gmail.com", "+1-555-0178", "alex#Pass99", false);
        Customer c4 = new Customer("Elena Rostova", "elena@vanguard.io", "+1-555-0133", "rostovaSecret", false);

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.persist(c3);
        entityManager.persist(c4);

        // 4. Orders
        Order o1 = new Order("ORD-2026-001", c1, OrderStatus.DELIVERED, new BigDecimal("1598.99"));
        o1.addItem(new OrderItem(o1, p1, 1, new BigDecimal("1199.00")));
        o1.addItem(new OrderItem(o1, p3, 1, new BigDecimal("399.99")));

        Order o2 = new Order("ORD-2026-002", c2, OrderStatus.SHIPPED, new BigDecimal("3499.00"));
        o2.addItem(new OrderItem(o2, p2, 1, new BigDecimal("3499.00")));

        Order o3 = new Order("ORD-2026-003", c3, OrderStatus.PENDING, new BigDecimal("1250.00"));
        o3.addItem(new OrderItem(o3, p5, 1, new BigDecimal("1250.00")));

        entityManager.persist(o1);
        entityManager.persist(o2);
        entityManager.persist(o3);

        entityManager.flush();
    }
}
