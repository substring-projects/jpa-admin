package io.github.jpaadmin.metadata;

import io.github.jpaadmin.annotations.InlineType;
import java.util.ArrayList;
import java.util.List;

public class InlineMetadata {
    private String propertyName;
    private String targetEntity;
    private InlineType type = InlineType.TABULAR;
    private String title;
    private boolean canAdd = true;
    private boolean canDelete = true;
    private int limit = 50;
    private String mappedBy;
    private List<FieldMetadata> fields = new ArrayList<>();

    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }

    public String getTargetEntity() { return targetEntity; }
    public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity; }

    public InlineType getType() { return type; }
    public void setType(InlineType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isCanAdd() { return canAdd; }
    public void setCanAdd(boolean canAdd) { this.canAdd = canAdd; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public String getMappedBy() { return mappedBy; }
    public void setMappedBy(String mappedBy) { this.mappedBy = mappedBy; }

    public List<FieldMetadata> getFields() { return fields; }
    public void setFields(List<FieldMetadata> fields) { this.fields = fields; }
}
