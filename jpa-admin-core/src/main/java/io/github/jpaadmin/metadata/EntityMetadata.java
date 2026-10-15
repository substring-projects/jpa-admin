package io.github.jpaadmin.metadata;

import io.github.jpaadmin.annotations.SortDir;
import java.util.ArrayList;
import java.util.List;

public class EntityMetadata {
    private String entityName;
    private String javaClassName;
    private String displayName;
    private String pluralName;
    private String group = "General";
    private String icon = "database";
    private int order = 100;
    private boolean readOnly = false;
    private boolean creatable = true;
    private boolean updatable = true;
    private boolean deletable = true;
    private boolean exportable = true;
    private String defaultSortBy = "id";
    private SortDir defaultSortDir = SortDir.DESC;
    private int pageSize = 25;
    private String primaryKeyField = "id";

    private List<FieldMetadata> fields = new ArrayList<>();
    private List<InlineMetadata> inlines = new ArrayList<>();
    private List<ActionMetadata> actions = new ArrayList<>();
    private List<String> roles = new ArrayList<>();

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getJavaClassName() { return javaClassName; }
    public void setJavaClassName(String javaClassName) { this.javaClassName = javaClassName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPluralName() { return pluralName; }
    public void setPluralName(String pluralName) { this.pluralName = pluralName; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }

    public boolean isCreatable() { return creatable; }
    public void setCreatable(boolean creatable) { this.creatable = creatable; }

    public boolean isUpdatable() { return updatable; }
    public void setUpdatable(boolean updatable) { this.updatable = updatable; }

    public boolean isDeletable() { return deletable; }
    public void setDeletable(boolean deletable) { this.deletable = deletable; }

    public boolean isExportable() { return exportable; }
    public void setExportable(boolean exportable) { this.exportable = exportable; }

    public String getDefaultSortBy() { return defaultSortBy; }
    public void setDefaultSortBy(String defaultSortBy) { this.defaultSortBy = defaultSortBy; }

    public SortDir getDefaultSortDir() { return defaultSortDir; }
    public void setDefaultSortDir(SortDir defaultSortDir) { this.defaultSortDir = defaultSortDir; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public String getPrimaryKeyField() { return primaryKeyField; }
    public void setPrimaryKeyField(String primaryKeyField) { this.primaryKeyField = primaryKeyField; }

    public List<FieldMetadata> getFields() { return fields; }
    public void setFields(List<FieldMetadata> fields) { this.fields = fields; }

    public List<InlineMetadata> getInlines() { return inlines; }
    public void setInlines(List<InlineMetadata> inlines) { this.inlines = inlines; }

    public List<ActionMetadata> getActions() { return actions; }
    public void setActions(List<ActionMetadata> actions) { this.actions = actions; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public FieldMetadata getField(String name) {
        return fields.stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public boolean hasField(String name) {
        return getField(name) != null;
    }

    public List<FieldMetadata> getSearchableFields() {
        return fields.stream().filter(FieldMetadata::isSearchable).toList();
    }
}
