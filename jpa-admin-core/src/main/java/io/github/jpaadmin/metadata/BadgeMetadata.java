package io.github.jpaadmin.metadata;

import io.github.jpaadmin.annotations.BadgeColor;

public class BadgeMetadata {
    private String value;
    private BadgeColor color;
    private String label;

    public BadgeMetadata() {}

    public BadgeMetadata(String value, BadgeColor color, String label) {
        this.value = value;
        this.color = color;
        this.label = (label != null && !label.isEmpty()) ? label : value;
    }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public BadgeColor getColor() { return color; }
    public void setColor(BadgeColor color) { this.color = color; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
