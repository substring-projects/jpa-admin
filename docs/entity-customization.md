# Entity customization

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

Annotations live in `com.substringtechnologies.jpaadmin.annotations`. Prefer field-based JPA mappings with annotations on fields for the current scanner. Working examples are available in [the sample domain package](../jpa-admin-sample-app/src/main/java/com/substringtechnologies/jpaadmin/sample/domain).

### Resource and field presentation

```java
import com.substringtechnologies.jpaadmin.annotations.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@AdminResource(
    displayName = "Product",
    pluralName = "Products",
    group = "Catalog",
    icon = "package",
    order = 10,
    pageSize = 25,
    defaultSortBy = "id",
    defaultSortDir = SortDir.DESC
)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @AdminField(label = "Product title", order = 1, searchable = true)
    private String name;

    @NotNull
    @Positive
    @AdminField(label = "Price", order = 2, filterable = true)
    private BigDecimal price;

    protected Product() {}
    // Add the accessors and business methods needed by your application.
}
```

| Annotation                  | Main options                                                                                                                       | Notes                                                                                                                              |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `@AdminResource`            | `displayName`, `pluralName`, `group`, `icon`, `order`, `pageSize`, `defaultSortBy`, `defaultSortDir`                               | Controls entity presentation and defaults.                                                                                         |
| `@AdminResource`            | `hidden`, `readOnly`, `creatable`, `updatable`, `deletable`                                                                        | `hidden` skips discovery. Entity-level CRUD flags are checked by the CRUD service; custom actions have separate behavior.          |
| `@AdminField`               | `label`, `description`, `placeholder`, `order`, `inList`, `inForm`, `inDetail`, `readOnly`, `searchable`, `sortable`, `filterable` | Controls metadata and UI. `filterable` fields appear in the list's filter panel: relations as a searchable picker, numbers and dates as ranges. |
| `@AdminWidget`              | `type`                                                                                                                             | `TEXTAREA`, `RICH_TEXT` (formatted editor; stored as HTML and sanitized before display), `CODE_EDITOR` (monospace editor with line numbers), `PASSWORD`, `BOOLEAN_SWITCH`, `COLOR_PICKER`, `IMAGE_URL`. `FILE_UPLOAD` is not implemented. |
| `@AdminBadge` / `@BadgeMap` | `value`, `color`, `label`                                                                                                          | Maps enum/string values to table badges.                                                                                           |
| `@AdminSensitive`           | Marker annotation                                                                                                                  | Masks non-null API values and hides the field from the list by default. Names containing `password` or `secret` are also detected. |
| `@AdminLookup`              | `lookupProperty`, `searchProperty`, `quickCreate`                                                                                  | Single-value relations and owning-side `@ManyToMany` collections (multi-select) are supported; some options remain incomplete, as explained below. |
| `@AdminInline`              | `type`, `title`, `canAdd`, `canDelete`, `limit`                                                                                    | `@OneToMany(mappedBy = ...)` collections are edited as rows inside the parent form and saved with it. Many-to-many inlines are read-only. |
| `@AdminUpload`             | `storage`, `folder`, `accept`, `maxSize`                                                                                           | Turns a `String` field into a file upload with drag and drop and live progress. The field stores the file's URL. See [File uploads](#file-uploads). |
| `@AdminComputed`            | `label`, `order`, `inList`, `inDetail`, `format`                                                                                   | Use conventional `get...` / `is...` getters. Custom formatting is not implemented.                                                 |
| `@AdminAction`              | `name`, `icon`, `confirmation`, `scope`                                                                                            | Exposes supported entity methods as actions.                                                                                       |
| `@AdminPermission`          | `roles`, `createRoles`, `readRoles`, `updateRoles`, `deleteRoles`                                                                  | Declared API; granular authorization is not implemented.                                                                           |

For complete option defaults, see [the annotation sources](../jpa-admin-core/src/main/java/com/substringtechnologies/jpaadmin/annotations).

### Status badges

```java
@Enumerated(EnumType.STRING)
@AdminField(label = "Status", filterable = true)
@AdminBadge(mapping = {
    @BadgeMap(value = "DRAFT", color = BadgeColor.GRAY, label = "Draft"),
    @BadgeMap(value = "PUBLISHED", color = BadgeColor.GREEN, label = "Published")
})
private ProductStatus status;
```

The enum values must exist in your application's `ProductStatus` enum.

### Single-value relationships

```java
@ManyToOne(fetch = FetchType.LAZY)
@AdminField(label = "Category")
@AdminLookup(lookupProperty = "name")
private Category category;
```

The record serializer uses `lookupProperty` for a related record's label. The lookup search endpoint prefers conventional display fields (`name`, `title`, `label`, `fullName`, `displayName`, `username`, `code`), then a non-sensitive string field, then the primary key; it also searches the primary key. It does not honor a relation-specific `searchProperty`. The UI requests up to 25 matches and supports searching, but not paged lookup results or quick-create dialogs.

### Sensitive fields

```java
@AdminSensitive
@AdminField(inList = false)
private String apiKey;
```

Sensitive values are returned as a mask rather than their original contents. This annotation does not encrypt or hash stored entity values. Admin account password handling and domain entity password handling are separate concerns.

### Inline collections

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
@AdminInline(title = "Order items", type = InlineType.TABULAR, canAdd = true, canDelete = true, limit = 50)
private java.util.List<OrderItem> items = new java.util.ArrayList<>();
```

The parent's edit form shows these rows as an editable table. Users can change existing rows, add rows when `canAdd` is true, and remove rows when `canDelete` is true. The rows are saved in the same transaction as the parent. Each child is bean-validated, and its errors are shown in the matching cell. The child's back-reference (`order` here) is set automatically and is not shown as a column. `limit` caps the number of rows. Many-to-many inlines stay read-only, because removing a row would delete a shared record.

### File uploads

```java
@AdminUpload(folder = "products", maxSize = "5MB")                       // images (default accept list)
private String imageUrl;

@AdminUpload(storage = "documents", accept = "application/pdf", maxSize = "10MB")
private String datasheetUrl;
```

| Option | Default | Meaning |
| --- | --- | --- |
| `storage` | `default` | Name of a storage under `spring.jpa.admin.storage` (see [configuration](configuration.md#file-storage)). |
| `folder` | none | Key prefix inside the storage. Files are saved as `folder/yyyy/MM/<random>-<original-name>.<ext>`. |
| `accept` | PNG, JPEG, WebP, GIF | Allowed MIME types. `image/*` style wildcards work; SVG is only accepted when `image/svg+xml` is listed explicitly. |
| `maxSize` | `5MB` | Size limit, such as `500KB`, `10MB` or `1GB`. |

The type and size are checked in the browser, checked again by the server before it issues the upload ticket, and verified after the upload, when the server reads the stored file's first bytes to confirm its real type. A file whose content doesn't match its type (for example HTML renamed to `.png`) is deleted and rejected. Replacing or removing a file only changes the record; the previous file stays in storage.

The application must start with every storage that an `@AdminUpload` field names configured. Otherwise startup fails with a message listing the fields.

### Many-to-many fields

```java
@ManyToMany
@JoinTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
@AdminLookup(lookupProperty = "name")
private Set<Tag> tags = new LinkedHashSet<>();
```

The owning side is edited with a searchable multi-select. The inverse side (`mappedBy`) is read-only, because only the owning side writes the join table. Detail pages list up to 200 linked records.

### Computed values

```java
@Transient
@AdminComputed(label = "In stock?", order = 9)
public boolean isInStock() {
    return stockQuantity != null && stockQuantity > 0;
}
```

Computed values are not persisted or writable. The serializer resolves standard getter names; arbitrary annotated method names and format strings are not fully supported.

### Custom actions

```java
@AdminAction(
    name = "Publish product",
    icon = "check-circle",
    confirmation = "Publish the selected products?",
    scope = ActionScope.BOTH
)
public void publish() {
    this.status = ProductStatus.PUBLISHED;
}
```

An instance no-argument method is invoked for each selected record. A static method taking a single `List` is also supported. Extra Spring service parameters are not injected. Available scopes are `SINGLE_ROW`, `BULK_SELECTION`, and `BOTH`; the UI uses them to place action controls.

Action IDs are returned in metadata. Clients should use those IDs rather than deriving them from display names. Actions must enforce their own business rules; resource read-only flags and UI action scope do not provide complete action authorization.

### Validation

The scanner recognizes common constraints including `@NotNull`, `@NotBlank`, `@NotEmpty`, `@Size`, `@Min`, `@Max`, `@Positive`, `@Email`, and `@Pattern`, plus some `@Column` attributes. The form uses required, length, numeric, and email attributes where applicable.

Validation is not an exact frontend replica of Bean Validation. Pattern metadata is not applied by the form, and `@Positive` is currently represented with a `0.01` minimum. Persistence-time validation remains authoritative; errors are returned as general messages rather than a structured field-error map.
