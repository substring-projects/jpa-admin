# Run the sample

<p class="jpa-lead">The repository includes a demo shop (products, categories, tags, customers and orders) with every feature switched on. It's the fastest way to see the console.</p>

## Requirements

| Tool | Version |
| --- | --- |
| JDK | 17 or newer |
| Maven | Included as the wrapper (`./mvnw`, or `mvnw.cmd` on Windows) |
| Node.js & npm | 20 or newer, needed only to build the UI from source |

## Build and start

```bash
npm ci --prefix jpa-admin-ui          # install the UI's locked dependencies
./mvnw clean package                  # build everything and run the Java tests
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-0.1.0.jar
```

Open **<http://localhost:8080/admin/>** and sign in with **`admin`** / **`admin`**.

!!! warning "Demo credentials"
    `admin` / `admin` is a fallback in the sample's `application.yml` for local use only. Override it with environment variables:
    `ADMIN_USERNAME=store-admin ADMIN_PASSWORD=… java -jar …`

## What to try

<div class="grid cards" markdown>

-   :material-view-dashboard-outline: __Dashboard__

    ---

    Record counts, the "Records created" chart, and records you recently viewed.

-   :material-filter-variant: __Products__

    ---

    Filter by category or price range, save the view, then edit a stock quantity in place.

-   :material-receipt-text-outline: __Orders__

    ---

    Open an order and edit its line items directly in the order form.

-   :material-image-outline: __Product images__

    ---

    Drag an image onto a product. Files are saved to `./uploads` and served from `/uploads`.

-   :material-tag-multiple-outline: __Tags__

    ---

    Assign several tags to a product with the many-to-many multi-select.

-   :material-keyboard-outline: __Shortcuts__

    ---

    Press ++cmd+k++ (or ++ctrl+k++) for the command palette, or <kbd>?</kbd> for every shortcut.

</div>

## Sample data

The sample uses an in-memory H2 database, seeded on every start with 3 categories, 5 products (images, tags, rich-text copy and a JSON spec on the first), 4 tags, 4 customers and 3 orders with line items. Data resets when the process restarts; uploaded files stay in `./uploads`.

The sample's entities in [`jpa-admin-sample-app/…/domain`](../jpa-admin-sample-app/src/main/java/com/substringtechnologies/jpaadmin/sample/domain) are a good reference for every annotation.

## Useful variations

```bash
# Another port
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-0.1.0.jar --server.port=8081

# Serve the console from /backoffice instead of /admin
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-0.1.0.jar --spring.jpa.admin.path=/backoffice

# Try your own brand color
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-0.1.0.jar --spring.jpa.admin.primary-color='#2563eb'
```

To work on the UI with live reload, see [Development](development.md#frontend-development).
