package com.substringtechnologies.jpaadmin.sample.domain;

import com.substringtechnologies.jpaadmin.annotations.AdminField;
import com.substringtechnologies.jpaadmin.annotations.AdminResource;
import com.substringtechnologies.jpaadmin.annotations.AdminWidget;
import com.substringtechnologies.jpaadmin.annotations.WidgetType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tags")
@AdminResource(displayName = "Tag", pluralName = "Tags", group = "Catalog", icon = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @AdminField(label = "ID", order = 1, sortable = true)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Size(max = 40)
    @AdminField(label = "Name", order = 2, searchable = true, sortable = true)
    private String name;

    @AdminField(label = "Color", order = 3)
    @AdminWidget(type = WidgetType.COLOR_PICKER)
    private String color;

    public Tag() {}

    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    @Override
    public String toString() { return name; }
}
