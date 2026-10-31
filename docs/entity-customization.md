# Customizing entities

<p class="jpa-lead">The console works without annotations. Add them to the entities you already have when you want better labels, grouping, badges, relations, uploads or actions.</p>

All annotations are in `com.substringtechnologies.jpaadmin.annotations`. Every option is listed in the [annotation reference](annotations.md); this page shows them in practice. The [sample entities](../jpa-admin-sample-app/src/main/java/com/substringtechnologies/jpaadmin/sample/domain) use all of them.

```java
import com.substringtechnologies.jpaadmin.annotations.*;
```

!!! tip "Annotate fields, not getters"
    The scanner reads annotations on fields. Keep JPA mappings and admin annotations on the same field.

## Name, group and order entities

```java
@Entity
@AdminResource(
    displayName = "Product",        // singular, used in buttons and messages ("New Product")
    pluralName = "Products",        // sidebar and page titles
    group = "Catalog",              // sidebar and dashboard section
    icon = "package",               // any Lucide icon name: https://lucide.dev/icons
    order = 10,                     // position inside the group (lower first)
    defaultSortBy = "createdAt",
    defaultSortDir = SortDir.DESC,
    pageSize = 25
)
public class Product { … }
```

Entities without `@AdminResource` still appear, in the **General** group, named after the class, with an icon guessed from the name.

## Control what users can do

```java
@AdminResource(
    readOnly = false,     // true: view only, no create/edit/delete
    creatable = true,
    updatable = true,
    deletable = false,    // e.g. keep an audit trail
    exportable = true,    // CSV export for this entity
    hidden = false        // true: leave the entity out of the console entirely
)
```

These flags are enforced by the API as well as the UI.

## Labels, order and visibility of fields

```java
@NotBlank
@AdminField(
    label = "Product title",
    description = "Shown to customers",   // help text under the input; tooltip on the record page
    placeholder = "e.g. Espresso Machine",
    order = 1,                            // lower comes first in tables, forms and record pages
    searchable = true,                    // included in the table search
    sortable = true,                      // clickable column header (default)
    filterable = true,                    // appears in the Filter panel
    inList = true,                        // show as a table column
    inForm = true,                        // show in create/edit forms
    inDetail = true,                      // show on the record page
    readOnly = false                      // shown but never editable
)
private String name;
```

!!! note "Automatic behavior"
    - Generated IDs and `@Column(updatable = false)` fields are read-only, and so are fields annotated with `@CreatedDate` or `@CreationTimestamp`.
    - Fields whose name contains `password` or `secret` are treated as sensitive.
    - Bean Validation annotations become form validation (see [Validation](#validation)).

## Status badges

```java
@Enumerated(EnumType.STRING)
@AdminField(label = "Status", filterable = true)
@AdminBadge(mapping = {
    @BadgeMap(value = "DRAFT", color = BadgeColor.GRAY, label = "Draft"),
    @BadgeMap(value = "PUBLISHED", color = BadgeColor.GREEN, label = "Published"),
    @BadgeMap(value = "OUT_OF_STOCK", color = BadgeColor.RED, label = "Out of stock")
})
private ProductStatus status;
```

Colors: `DEFAULT`, `PRIMARY`, `BLUE`, `GREEN`, `YELLOW`, `RED`, `PURPLE`, `INDIGO`, `PINK`, `GRAY`. Enums without a mapping still get colored badges with readable labels automatically.

## Widgets

```java
@AdminWidget(type = WidgetType.TEXTAREA)      // multi-line text
private String description;

@AdminWidget(type = WidgetType.RICH_TEXT)     // formatted text, stored as HTML
private String marketingCopy;

@AdminWidget(type = WidgetType.CODE_EDITOR)   // JSON, SQL, templates…
private String specifications;

@AdminWidget(type = WidgetType.COLOR_PICKER)  // "#16a34a"
private String color;

@AdminWidget(type = WidgetType.IMAGE_URL)     // a link to an image, shown as a picture
private String externalImage;
```

Rich text is sanitized before it's displayed, so stored HTML can't run scripts in the console. Use [`@AdminUpload`](file-uploads.md) instead of `IMAGE_URL` when users should upload the image.

## Relationships

### Many-to-one and one-to-one

```java
@ManyToOne(fetch = FetchType.LAZY)
@AdminField(label = "Category", filterable = true)
@AdminLookup(lookupProperty = "name")   // which property labels the related record
private Category category;
```

Edited with a searchable picker. The label comes from `lookupProperty`, then from a conventional name field (`name`, `title`, `label`, `fullName`, `displayName`, `username`, `code`), then from `toString()`, then the ID. Tables link to the related record, and `filterable = true` adds a relation filter.

### Many-to-many

```java
@ManyToMany
@JoinTable(name = "product_tags",
    joinColumns = @JoinColumn(name = "product_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id"))
@AdminField(label = "Tags")
@AdminLookup(lookupProperty = "name")
private Set<Tag> tags = new LinkedHashSet<>();
```

The owning side is edited with a multi-select. The inverse side (`mappedBy`) is read-only, because only the owning side writes the join table. Record pages list up to 200 linked records.

### One-to-many child rows

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
@AdminInline(title = "Order line items", canAdd = true, canDelete = true, limit = 50)
private List<OrderItem> items = new ArrayList<>();
```

Rows are edited inside the parent's form and saved in the same transaction. The back-reference (`order`) is filled in automatically. Each row is validated, and its errors appear in the matching cell. The record page shows the rows in their own tab.

!!! warning "Inline collections need `mappedBy`"
    Only parent-owned one-to-many collections are editable inline. Many-to-many inlines are shown read-only, because deleting a row would delete a shared record. One-to-many collections without `@AdminInline` aren't shown.

## File and image uploads

```java
@AdminUpload(folder = "products", maxSize = "5MB")
private String imageUrl;

@AdminUpload(storage = "documents", accept = "application/pdf", maxSize = "10MB")
private String datasheetUrl;
```

Covered in depth in [File & image uploads](file-uploads.md).

## Sensitive fields

```java
@AdminSensitive
private String apiKey;
```

Sensitive values are masked in API responses, hidden from tables, and edited with a masked input; leaving it blank keeps the current value. This is a display safeguard. It doesn't encrypt or hash the stored value.

## Computed values

```java
@Transient
@AdminComputed(label = "In stock?", order = 9)
public boolean isInStock() {
    return stockQuantity != null && stockQuantity > 0;
}
```

Shown in tables and on record pages, never editable or persisted. Use a standard `getX()` / `isX()` getter name.

## Custom actions

```java
@AdminAction(
    name = "Publish product",
    confirmation = "Publish the selected products?",
    scope = ActionScope.BOTH          // SINGLE_ROW, BULK_SELECTION or BOTH
)
public void publish() {
    this.status = ProductStatus.PUBLISHED;
}
```

- A no-argument **instance method** runs once per selected record, inside a transaction, and the changes are saved. The console then shows "Publish product completed".
- A **static method taking a `List`** receives all selected records at once. If it returns a `String`, that text is shown as the success message.
- `SINGLE_ROW` actions appear in the row and record menus, `BULK_SELECTION` in the bulk bar, and `BOTH` in all of them.

Actions run with the admin's permissions. Enforce your own business rules inside the method.

## Validation

Standard Bean Validation annotations drive both the form and the server:

| Annotation | In the form | On the server |
| --- | --- | --- |
| `@NotNull`, `@NotBlank`, `@NotEmpty` | Required marker and "is required" | ✓ |
| `@Size(min, max)` | Length checks | ✓ |
| `@Min`, `@Max` | Range checks | ✓ |
| `@Positive` | Must be greater than 0 | ✓ |
| `@Email` | Email check and email keyboard | ✓ |
| `@Pattern` | Pattern check | ✓ |
| Any other constraint | — | ✓ (shown next to the field) |

The server runs the entity's full Bean Validation before saving and returns every problem at once as [field errors](api-reference.md#errors). A rejected save never changes the database.
