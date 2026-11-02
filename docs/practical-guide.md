# Build a useful admin console

Start with a small, intentional set of editable data, give people the permissions their work needs, and then add conveniences. This walkthrough connects the configuration, annotations and permission model to a practical catalog-management workflow. Identity features here require version 0.2.0 or newer.

## 1. Choose an account model

| Situation | Use |
| --- | --- |
| One trusted operator, local exploration or a small internal tool | `security.mode: configured` and an environment-supplied password |
| Several operators with different access | `security.mode: database` plus `jpa-admin-identity` |
| Another identity database or custom persistence | `security.mode: database` plus a custom `AdminIdentityStore` bean |
| No admin console in this deployment | `spring.jpa.admin.enabled: false` |

The default **library** mode is `configured`; the current **sample** explicitly enables `database`. Adding the identity dependency alone does not switch a host application's mode. A host security role or JWT does not grant admin access.

## 2. Review everything that will be discovered

Every JPA entity in the metamodel is discovered automatically, even without `@AdminResource`. Review the entity list before giving access to a configured administrator or superuser.

```java
@Entity
@AdminResource(hidden = true)
public class InternalCredential {
    // Existing JPA fields and mappings.
}
```

This excludes the entity from the admin registry and permission catalog. It remains usable by the host's own repositories and APIs. Removing the annotation does not hide the entity. `group = "Catalog"` is a sidebar category, not a security group.

For an entity that everyone should only inspect:

```java
@AdminResource(readOnly = true)
```

This is an entity-wide restriction, including superusers. To restrict only a particular team, keep the entity editable and grant that team only View.

## 3. Make one entity pleasant to manage

Apply annotations to your existing entity. For example:

```java
@Entity
@AdminResource(
    displayName = "Product",
    pluralName = "Products",
    group = "Catalog",
    icon = "package",
    permissionResource = "catalog.Product",
    defaultSortBy = "name",
    defaultSortDir = SortDir.ASC,
    pageSize = 25,
    deletable = false
)
public class Product {
    @Id @GeneratedValue
    private Long id;

    @NotBlank
    @AdminField(label = "Product name", searchable = true, order = 10)
    private String name;

    @NotNull @DecimalMin("0.00")
    @AdminField(filterable = true, order = 20)
    private BigDecimal price;

    @ManyToOne
    @AdminLookup(lookupProperty = "name")
    @AdminField(filterable = true, order = 30)
    private Category category;

    // Keep your normal constructors, accessors and other JPA mappings.
}
```

Imports come from `jakarta.persistence`, `jakarta.validation.constraints`, `java.math.BigDecimal` and `com.substringtechnologies.jpaadmin.annotations`; `Category` is your existing entity. This is an excerpt to adapt, not a complete standalone domain model.

- Set a stable `permissionResource` before creating groups. Java package/class renames otherwise change default permission keys. Entity API URLs still use the entity name, not this permission namespace.
- Choose a sort field that actually exists. `pageSize` overrides the global `page-size` property for one entity.
- Mark a few useful text fields searchable. If none are marked, search falls back to all text fields; it is not a database full-text search engine.
- Mark useful fields filterable; the UI derives controls from their types.
- Use `@AdminLookup` for readable relation names. Avoid expensive or sensitive `toString()` implementations and computed getters; they are application code executed while rendering data.
- Bean Validation is checked by the backend even when a widget does not expose a matching client-side limit. `@AdminWidget(min, max)` bounds are enforced by the backend too.

## 4. Create permission groups around jobs

Log in as a database superuser, open **Users and groups → Groups**, and create the groups your operators need:

| Example group | Grants | Outcome |
| --- | --- | --- |
| Catalog Readers | Product View; Category View; Tag View | Browse catalog and related labels |
| Catalog Editors | Reader grants + Product Add/Change | Create/edit products and select existing categories/tags |
| Order Operators | Order View/Change; required OrderItem View/Add/Change/Delete | Work on an order and its editable inline items, subject to entity and inline flags |

If the sample's resource names are still defaults, select entities in the UI instead of copying `catalog.Product:*` keys from this example. Wildcards such as `*` are not supported.

Creating a product linked to an existing category needs Category View, not necessarily Category Add. Creating or editing a child inline record requires the corresponding child permission as well as access to the parent. Do not grant broad superuser access just to make a relation picker appear.

The permissions matrix adds View when a write or action is selected. Clearing View clears all permissions for that entity **in the group being edited**. Other groups may still grant access; grants are combined and there are no explicit denies. Entity hard restrictions still apply even if a matching permission can be selected in the catalog.

Create a named account for each operator, assign its groups, and leave Superuser unchecked unless the person must manage security. Active and Allow admin access must both be enabled. No group means no entity permissions for a non-superuser, though they may still sign in to the shell.

## 5. Test as the actual operator

Use a separate browser profile or incognito session, not another tab sharing the same cookies.

1. Sign in as a reader: only permitted entities should appear; editing controls should be absent.
2. Sign in as an editor: create/edit a disposable record, including its relations and inline children.
3. Try a direct write request without a matching grant: the backend should return `403 ACCESS_DENIED` even if someone constructs the URL manually.
4. Revoke a grant as the superuser. The user's next request should obey the new grant set; refresh to update visible controls.
5. Disable a test account or reset its password. Its existing session should no longer be valid.

Changing a group does not force a sign-out; changing password, username, Active or Allow admin access changes the account version and invalidates sessions. A superuser's group assignments do not limit their entity access. [Permission boundaries](permissions.md#indirect-access-and-boundaries) explain actions, uploads and cascades.

## 6. Use daily-work features deliberately

- Save commonly used search/filter/sort combinations as named views, such as “Low stock”. Views and column settings are browser-local, not team-wide policies.
- Use compact density for routine tables and hide noisy columns. This affects presentation, not authorization.
- Prefer forms for coordinated multi-field changes; edit-in-place saves individual changes immediately.
- Bulk selection selects the current page, not every matching record across pages. Review selected records before running an action.
- Duplicate creates a prefilled new-record form; validate unique fields before saving.
- CSV export follows readable data. `exportable=false` hides the button; it cannot prevent an authorized reader copying API data.
- Give custom actions clear names and confirmation text for consequential operations. Action methods and external side effects remain application code; the library cannot automatically infer their business authorization rules.
- Use explicit `@AdminSensitive` on sensitive fields instead of relying only on names. Masking is not encryption and a generic PASSWORD widget does not implement a host application's password-hashing workflow. Identity account passwords have a separate BCrypt flow.

## 7. Connect host security without interference

Reserve `/admin` (or your configured prefix) for the library. Keep normal APIs under another prefix such as `/api`. The admin chain is ordered at `-100`; a host catch-all chain must run later or exclude that prefix. Keep custom JWT filters scoped to the host chain, including disabling global servlet registration when appropriate.

The admin does not accept a host bearer token. Host APIs do not automatically gain protection because the starter is installed. See [host Spring Security integration](security.md#coexisting-with-host-spring-security) for the complete boundary and shared-session limitations.

## 8. Prepare the deployment

Use the [database guide](database-setup.md) for driver dependencies and migrations. Use a persistent datasource for admin identities, configure HTTPS and your host security, and supply secrets through your platform. Back up both business data and identity tables before upgrades. Store uploads on durable storage independently of the database backup.

There is no built-in login rate limiting, audit history, MFA, row-level tenancy enforcement or SSO account mapping. Check [limitations](limitations.md) against your requirements before relying on the console for those tasks. Scope a first rollout to a small team and a small set of permissions, then expand after exercising the actual workflows.
