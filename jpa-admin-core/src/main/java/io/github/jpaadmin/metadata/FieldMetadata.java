package io.github.jpaadmin.metadata;

import io.github.jpaadmin.annotations.WidgetType;
import java.util.ArrayList;
import java.util.List;

public class FieldMetadata {
    private String name;
    private String javaType;
    private FieldType fieldType;
    private String label;
    private String description;
    private int order = 100;
    private boolean inList = true;
    private boolean inForm = true;
    private boolean inDetail = true;
    private boolean readOnly = false;
    private boolean searchable = false;
    private boolean sortable = true;
    private boolean filterable = false;
    private String placeholder = "";
    private boolean primaryKey = false;
    private boolean generatedValue = false;
    private boolean sensitive = false;
    private WidgetType widget = WidgetType.AUTO;
    private ValidationMetadata validation = new ValidationMetadata();
    private List<String> enumValues = new ArrayList<>();
    private List<BadgeMetadata> badges = new ArrayList<>();

    // Relational fields (@ManyToOne, @OneToOne)
    private String lookupTargetEntity;
    private String lookupProperty;
    private String searchProperty;
    private boolean quickCreate = false;

    // Computed / Virtual
    private boolean computed = false;
    private String format = "";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getJavaType() { return javaType; }
    public void setJavaType(String javaType) { this.javaType = javaType; }

    public FieldType getFieldType() { return fieldType; }
    public void setFieldType(FieldType fieldType) { this.fieldType = fieldType; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public boolean isInList() { return inList; }
    public void setInList(boolean inList) { this.inList = inList; }

    public boolean isInForm() { return inForm; }
    public void setInForm(boolean inForm) { this.inForm = inForm; }

    public boolean isInDetail() { return inDetail; }
    public void setInDetail(boolean inDetail) { this.inDetail = inDetail; }

    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }

    public boolean isSearchable() { return searchable; }
    public void setSearchable(boolean searchable) { this.searchable = searchable; }

    public boolean isSortable() { return sortable; }
    public void setSortable(boolean sortable) { this.sortable = sortable; }

    public boolean isFilterable() { return filterable; }
    public void setFilterable(boolean filterable) { this.filterable = filterable; }

    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }

    public boolean isPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

    public boolean isGeneratedValue() { return generatedValue; }
    public void setGeneratedValue(boolean generatedValue) { this.generatedValue = generatedValue; }

    public boolean isSensitive() { return sensitive; }
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }

    public WidgetType getWidget() { return widget; }
    public void setWidget(WidgetType widget) { this.widget = widget; }

    public ValidationMetadata getValidation() { return validation; }
    public void setValidation(ValidationMetadata validation) { this.validation = validation; }

    public List<String> getEnumValues() { return enumValues; }
    public void setEnumValues(List<String> enumValues) { this.enumValues = enumValues; }

    public List<BadgeMetadata> getBadges() { return badges; }
    public void setBadges(List<BadgeMetadata> badges) { this.badges = badges; }

    public String getLookupTargetEntity() { return lookupTargetEntity; }
    public void setLookupTargetEntity(String lookupTargetEntity) { this.lookupTargetEntity = lookupTargetEntity; }

    public String getLookupProperty() { return lookupProperty; }
    public void setLookupProperty(String lookupProperty) { this.lookupProperty = lookupProperty; }

    public String getSearchProperty() { return searchProperty; }
    public void setSearchProperty(String searchProperty) { this.searchProperty = searchProperty; }

    public boolean isQuickCreate() { return quickCreate; }
    public void setQuickCreate(boolean quickCreate) { this.quickCreate = quickCreate; }

    public boolean isComputed() { return computed; }
    public void setComputed(boolean computed) { this.computed = computed; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
