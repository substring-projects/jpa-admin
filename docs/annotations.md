# Annotation reference

<p class="jpa-lead">Every annotation and option, with its default and current status. Package: <code>com.substringtechnologies.jpaadmin.annotations</code>.</p>

<span class="jpa-pill jpa-pill--yes">Supported</span> works as described ·
<span class="jpa-pill jpa-pill--partial">Partial</span> works with limits noted ·
<span class="jpa-pill jpa-pill--no">Not yet</span> accepted but has no effect yet

## @AdminResource

On an entity class. Controls how the entity appears and what can be done with it.

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| `permissionResource` | `String` | fully qualified Java class name | Supported | Stable case-sensitive grant namespace, for example `catalog.Product`. Does not rename entity API URLs. |
| `displayName` | `String` | class name, humanized | <span class="jpa-pill jpa-pill--yes">Supported</span> | Singular name ("New Product"). |
| `pluralName` | `String` | display name + "s" | <span class="jpa-pill jpa-pill--yes">Supported</span> | Sidebar and page title. |
| `group` | `String` | `"General"` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Sidebar and dashboard group. |
| `icon` | `String` | `"database"` | <span class="jpa-pill jpa-pill--yes">Supported</span> | A [Lucide](https://lucide.dev/icons) icon name such as `shopping-cart`. With the default, an icon is chosen from the entity name. |
| `order` | `int` | `100` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Position within the group, lowest first. |
| `readOnly` | `boolean` | `false` | <span class="jpa-pill jpa-pill--yes">Supported</span> | View only. Enforced by the API. |
| `creatable` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Allow creating records. Enforced by the API. |
| `updatable` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Allow editing. Enforced by the API. |
| `deletable` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Allow deleting. Enforced by the API. |
| `exportable` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Show the CSV Export button. |
| `defaultSortBy` | `String` | `"id"` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Field the table is sorted by initially. |
| `defaultSortDir` | `SortDir` | `DESC` | <span class="jpa-pill jpa-pill--yes">Supported</span> | `ASC` or `DESC`. |
| `pageSize` | `int` | `0` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Initial rows per page, 1–500. `0` uses [`spring.jpa.admin.page-size`](configuration.md). |
| `hidden` | `boolean` | `false` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Exclude the entity from the console and API. |

## @AdminField

On an entity field.

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| `label` | `String` | field name, humanized | <span class="jpa-pill jpa-pill--yes">Supported</span> | Column, form and detail label. |
| `description` | `String` | `""` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Help text under the form input; tooltip on the record page. |
| `placeholder` | `String` | `""` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Input placeholder. |
| `order` | `int` | `100` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Position in tables, forms and record pages (the ID is always first). |
| `inList` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Show as a table column. |
| `inForm` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Show in create and edit forms. |
| `inDetail` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Show on the record page. |
| `readOnly` | `boolean` | `false` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Display only; ignored in update requests. |
| `searchable` | `boolean` | `false` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Include in the table search (text fields). If no field is searchable, all text fields are searched. |
| `sortable` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Allow sorting by this column. |
| `filterable` | `boolean` | `false` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Add to the Filter panel (text, enum, boolean, relation, number range, date range). |

## @AdminWidget

On a field. Chooses the input control.

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| `type` | `WidgetType` | `AUTO` | <span class="jpa-pill jpa-pill--yes">Supported</span> | See the widget table below. |
| `min`, `max` | `double` | unbounded | <span class="jpa-pill jpa-pill--yes">Supported</span> | Number bounds. The input enforces them and the API rejects values outside them with a field error. Numeric fields only. |
| `step` | `double` | `0` (any) | <span class="jpa-pill jpa-pill--yes">Supported</span> | Input increment for numbers and sliders. |
| `language` | `String` | `"json"` | <span class="jpa-pill jpa-pill--yes">Supported</span> | `CODE_EDITOR` syntax: `json`, `javascript`, `typescript`, `html`, `css`, `sql`, `xml`, `markdown`, `yaml`, `python` or `text`. |
| `previewSize` | `String` | `""` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Thumbnail size in tables for image fields, as a CSS length (`64px`, `4rem`). |

Misconfigured widgets fail at startup and name the field: bounds or `SLIDER` on a non-numeric field, `min` above `max`, a `SLIDER` without both bounds, an unknown `language` or an invalid `previewSize`.

| `WidgetType` | Status | Renders as |
| --- | --- | --- |
| `AUTO` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Chosen from the Java type |
| `TEXT` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Single-line input |
| `TEXTAREA` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Multi-line input |
| `RICH_TEXT` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Formatting editor; HTML is sanitized on display |
| `CODE_EDITOR` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Editor with line numbers |
| `COLOR_PICKER` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Color picker plus hex input; swatch in tables |
| `IMAGE_URL` | <span class="jpa-pill jpa-pill--yes">Supported</span> | URL input; image preview in tables and on record pages |
| `PASSWORD` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Masked input with reveal |
| `BOOLEAN_SWITCH` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Switch |
| `DATE_PICKER`, `DATE_TIME_PICKER` | <span class="jpa-pill jpa-pill--partial">Partial</span> | Same as `AUTO`: date fields already get native pickers |
| `SLIDER` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Range slider with the current value; requires `min` and `max` |
| `FILE_UPLOAD` | <span class="jpa-pill jpa-pill--partial">Partial</span> | Use [`@AdminUpload`](#adminupload), which sets this automatically |

## @AdminUpload

On a `String` field that stores the uploaded file's URL. See [File & image uploads](file-uploads.md).

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| `storage` | `String` | `"default"` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Name of a storage under `spring.jpa.admin.storage`. Startup fails if it isn't configured. |
| `folder` | `String` | `""` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Key prefix. Files are stored as `folder/yyyy/MM/<random>-<name>.<ext>`. |
| `accept` | `String[]` | PNG, JPEG, WebP, GIF | <span class="jpa-pill jpa-pill--yes">Supported</span> | Allowed MIME types; `image/*` wildcards work. SVG only when `image/svg+xml` is listed. |
| `maxSize` | `String` | `"5MB"` | <span class="jpa-pill jpa-pill--yes">Supported</span> | `500KB`, `10MB`, `1GB` or a byte count. |

## @AdminBadge and @BadgeMap

On an enum or string field. Maps values to colored badges.

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| `@AdminBadge.mapping` | `BadgeMap[]` | `{}` | <span class="jpa-pill jpa-pill--yes">Supported</span> | One entry per value. |
| `@BadgeMap.value` | `String` | — (required) | <span class="jpa-pill jpa-pill--yes">Supported</span> | Stored value, case-insensitive. |
| `@BadgeMap.color` | `BadgeColor` | `DEFAULT` | <span class="jpa-pill jpa-pill--yes">Supported</span> | `DEFAULT`, `PRIMARY`, `BLUE`, `GREEN`, `YELLOW`, `RED`, `PURPLE`, `INDIGO`, `PINK`, `GRAY`. |
| `@BadgeMap.label` | `String` | `""` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Text shown in the badge. |

## @AdminLookup

On a relation field.

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| `lookupProperty` | `String` | `""` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Property used as the related record's label. |
| `searchProperty` | `String` | `""` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Text property of the related record the picker searches, plus the ID. Without it the picker searches a name-like field. |

`lookupProperty` and `searchProperty` must name visible (not sensitive or computed) fields of the related entity, and `searchProperty` a text field; otherwise startup fails. The picker labels options with `lookupProperty` too.

## @AdminInline

On a `@OneToMany` or `@ManyToMany` collection.

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| `title` | `String` | field name, humanized | <span class="jpa-pill jpa-pill--yes">Supported</span> | Section and tab title. |
| `canAdd` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Allow adding rows. Enforced by the API. |
| `canDelete` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Allow removing rows. Enforced by the API. |
| `limit` | `int` | `50` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Maximum rows. Enforced by the API. |
| `type` | `InlineType` | `TABULAR` | <span class="jpa-pill jpa-pill--yes">Supported</span> | `TABULAR` edits rows in a table; `STACKED` shows each row as a panel with full-size inputs, better for many or wide fields. |

Editable only for `@OneToMany(mappedBy = …)`. Many-to-many inlines are read-only.

## @AdminAction

On an entity method. See [Custom actions](entity-customization.md#custom-actions).

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| `name` | `String` | — (required) | <span class="jpa-pill jpa-pill--yes">Supported</span> | Menu and button label. |
| `confirmation` | `String` | `""` | <span class="jpa-pill jpa-pill--yes">Supported</span> | When set, asks for confirmation with this message. |
| `scope` | `ActionScope` | `BOTH` | <span class="jpa-pill jpa-pill--yes">Supported</span> | `SINGLE_ROW`, `BULK_SELECTION` or `BOTH`. Controls where the action appears. |
| `icon` | `String` | `"zap"` | <span class="jpa-pill jpa-pill--yes">Supported</span> | [Lucide](https://lucide.dev/icons/) icon name, such as `send` or `archive`. Unknown names show the default icon. |

An action that throws `IllegalStateException` or `IllegalArgumentException` shows the exception's message to the admin (HTTP 409); use it for expected refusals such as "Archived products can't be published". Other exceptions are logged and reported as a generic server error.

## @AdminComputed

On a no-argument `getX()` / `isX()` method.

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| `label` | `String` | property name, humanized | <span class="jpa-pill jpa-pill--yes">Supported</span> | Column and detail label. |
| `order` | `int` | `100` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Position. |
| `inList` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Show as a column. |
| `inDetail` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Show on the record page. |
| `format` | `String` | `""` | <span class="jpa-pill jpa-pill--yes">Supported</span> | [`java.util.Formatter`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Formatter.html) pattern applied on the server, such as `"$%,.2f"` or `"%.1f%%"`. A pattern that doesn't fit the value logs a warning and shows the plain value. |

## @AdminSensitive

On a field. The value is write-only: it is never sent to the browser, searched, filtered, sorted or exported. Forms use a masked input; leaving it blank keeps the stored value.

| Option | Type | Default | Status | Description |
| --- | --- | --- | --- | --- |
| *(marker)* | | | <span class="jpa-pill jpa-pill--yes">Supported</span> | Fields named like `password` or `secret` are treated as sensitive automatically. |
| `mask` | `boolean` | `true` | <span class="jpa-pill jpa-pill--yes">Supported</span> | `true` shows `maskPattern` when a value is set. `false` doesn't reveal whether a value is set; the console shows "Hidden". |
| `maskPattern` | `String` | `"••••••••"` | <span class="jpa-pill jpa-pill--yes">Supported</span> | Text shown for a set value. A request that sends this text back is treated as "unchanged". |

Set `@AdminResource(permissionResource = "catalog.Product")` for a stable permission namespace; the default is the fully qualified entity class name. See [Permissions](permissions.md).

## Enums

| Enum | Values |
| --- | --- |
| `SortDir` | `ASC`, `DESC` |
| `ActionScope` | `SINGLE_ROW`, `BULK_SELECTION`, `BOTH` |
| `InlineType` | `TABULAR`, `STACKED` |
| `BadgeColor` | `DEFAULT`, `PRIMARY`, `BLUE`, `GREEN`, `YELLOW`, `RED`, `PURPLE`, `INDIGO`, `PINK`, `GRAY` |
| `WidgetType` | `AUTO`, `TEXT`, `TEXTAREA`, `RICH_TEXT`, `COLOR_PICKER`, `SLIDER`, `CODE_EDITOR`, `DATE_PICKER`, `DATE_TIME_PICKER`, `FILE_UPLOAD`, `IMAGE_URL`, `PASSWORD`, `BOOLEAN_SWITCH` |
